package tourGuide.service;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.task.CalculateRewardTask;
import tourGuide.task.UserLocationTask;
import tourGuide.user.User;
import tourGuide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {

		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		//TODO Envoyer un future?
		System.out.println("calculateRewards before for loop in Rewards service" + "by" + Thread.currentThread().getName());
		for (VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				//To check by attraction name if the user's reward is already added, if not, it is added
				//TODO à tester sans if ci dessous
				if (user.getUserRewards().stream().noneMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName))) {
					if (nearAttraction(visitedLocation, attraction)) {
						user.addUserReward(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
						System.out.println("addUserReward Rewards service" + "by" + Thread.currentThread().getName());
					}
				}
			}
		}

	}
	public void calculateRewardsForkJoin(List<User> users) {

		for (User user: users) {
			List<VisitedLocation> userLocations = user.getVisitedLocations();
			List<Attraction> attractions = gpsUtil.getAttractions();
            // Create ForkJoin pool
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
            // Create our first task
            CalculateRewardTask calculateRewardTask = new CalculateRewardTask(user, userLocations, attractions);
            // 4. Invoke the job in the pool
            commonPool.invoke(calculateRewardTask);
			//System.out.println("calculateRewards before for loop in Rewards service" + "by" + Thread.currentThread().getName());

		}

	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	public boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
