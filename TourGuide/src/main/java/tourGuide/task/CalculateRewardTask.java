package tourGuide.task;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import rewardCentral.RewardCentral;
import tourGuide.service.RewardsService;
import tourGuide.user.User;
import tourGuide.user.UserReward;

import java.util.List;
import java.util.concurrent.RecursiveAction;

public class CalculateRewardTask extends RecursiveAction {

    private static final int SEQUENTIAL_THRESHOLD = 1;

    private User user;

    private List<VisitedLocation> visitedLocations;

    private List<Attraction> attractions;

    private  GpsUtil gpsUtil = new GpsUtil();
    private  RewardCentral rewardsCentral = new RewardCentral();

    @Autowired
    private RewardsService rewardsService = new RewardsService(gpsUtil, rewardsCentral);


    public CalculateRewardTask(User user, List<VisitedLocation> visitedLocations, List<Attraction> attractions) {
        this.user = user;
        this.visitedLocations = visitedLocations;
        this.attractions = attractions;
    }

    @Override
    protected void compute() {
        if (visitedLocations.size() == SEQUENTIAL_THRESHOLD) { // base case

            calculateRewarsPoints();

        } else { // recursive case


            int midPoint = visitedLocations.size() / 2;

            CalculateRewardTask firstHalfSubtask = new CalculateRewardTask(user, visitedLocations.subList(0, midPoint), attractions);

            CalculateRewardTask secondHalfSubtask = new CalculateRewardTask(user, visitedLocations.subList(midPoint, visitedLocations.size()), attractions);

            //asynchronously work out the left side
            firstHalfSubtask.fork();

            // Work out the right hand side in this thread.
            secondHalfSubtask.compute();

            // Get back the left side when it's ready
            firstHalfSubtask.join(); // wait for the first task result



        }
    }

    private void calculateRewarsPoints () {
        for (VisitedLocation visitedLocation : visitedLocations) {
            for (Attraction attraction : attractions) {
                if (user.getUserRewards().stream().noneMatch(reward -> reward.attraction.attractionName.equals(attraction.attractionName))) {
                    if (rewardsService.nearAttraction(visitedLocation, attraction)) {
                        user.addUserReward(new UserReward(visitedLocation, attraction, rewardsService.getRewardPoints(attraction, user)));
                    }
                }
            }
        }
    }
}
