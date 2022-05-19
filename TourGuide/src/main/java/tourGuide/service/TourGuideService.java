package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.DTO.AttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.task.CalculateRewardTask;
import tourGuide.task.UserLocationTask;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;
import tourGuide.user.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;


import java.util.Locale;

@Service
public class TourGuideService {
    private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public Tracker tracker;
    boolean testMode = true;


    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, Tracker tracker) {

        // Test correction of NumberFormatException
        Locale.setDefault(Locale.US);

        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        this.tracker = tracker;
        logger.debug("New tracker instance");
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) {
        VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ?
                user.getLastVisitedLocation() :
                trackUserLocation(user);
        return visitedLocation;
    }

    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return internalUserMap.values().stream().collect(Collectors.toList());
    }

    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    public List<Provider> getTripDeals(User user) {
        int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

    public VisitedLocation trackUserLocation(User user) {
        logger.debug("trackUserLocation");
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    public VisitedLocation trackUserLocationCompletableFuture(User user) throws ExecutionException, InterruptedException {
        logger.debug("trackUserLocation CompletableFuture");
       // Executor executor = Executors.newFixedThreadPool(20);
        CompletableFuture<VisitedLocation> getUserLocationFuture = CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()));
        getUserLocationFuture.thenAccept(user::addToVisitedLocations).thenRunAsync(() ->  rewardsService.calculateRewards(user));
        return getUserLocationFuture.get();
    }
    public VisitedLocation trackUserLocationForkJoin(List<User> users) {
        logger.debug("trackUserLocation ForkJoin");
        // Create ForkJoin pool
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        // Create our first task
        UserLocationTask userLocationTask = new UserLocationTask(users);
        // 4. Invoke the job in the pool
        VisitedLocation visitedLocationResult = commonPool.invoke(userLocationTask);

        rewardsService.calculateRewardsForkJoin(users);

        return visitedLocationResult;
    }

    public List<AttractionDTO> getNearByAttractions(VisitedLocation visitedLocation, User user) {

        List<AttractionDTO> nearbyAttractions = new ArrayList<>();
        Map<Double, Attraction> attractionDistance = new HashMap<>();

        for (Attraction attraction : gpsUtil.getAttractions()) {
            double distance = rewardsService.getDistance(attraction, visitedLocation.location);
            attractionDistance.put(distance, attraction);
        }
        attractionDistance.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(5)
                .forEach(attraction -> attractionToAttractionDTO(attraction.getValue(), visitedLocation, user, attraction.getKey(), nearbyAttractions));

        return nearbyAttractions;
    }

    private void attractionToAttractionDTO(Attraction attraction, VisitedLocation visitedLocation, User user, double distance, List<AttractionDTO> nearbyAttractions) {
        AttractionDTO attractionDTO = new AttractionDTO();
        attractionDTO.setAttractionName(attraction.attractionName);
        attractionDTO.setAttractionLocation(attraction);
        attractionDTO.setUserLocation(visitedLocation.location);
        attractionDTO.setDistance(distance);
        attractionDTO.setRewardPoints(rewardsService.getRewardPoints(attraction, user));
        nearbyAttractions.add(attractionDTO);
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                 tracker.stopTracking();
            }
        });
    }

    /**********************************************************************************
     *
     * Methods Below: For Internal Testing
     *
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
