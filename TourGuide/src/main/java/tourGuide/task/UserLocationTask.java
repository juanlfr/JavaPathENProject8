package tourGuide.task;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;
import org.springframework.beans.factory.annotation.Autowired;
import tourGuide.user.User;

import java.util.List;
import java.util.concurrent.RecursiveTask;

public class UserLocationTask extends RecursiveTask<VisitedLocation> {

    private static final int SEQUENTIAL_THRESHOLD = 200;

    private GpsUtil gpsUtil = new GpsUtil();

    private List<User> users;

    public UserLocationTask(List<User> users) {
        this.users = users;
    }

    @Override
    protected VisitedLocation compute() {
        if (users.size() <= SEQUENTIAL_THRESHOLD) { // base case

            return getUserVisitedLocation();

        } else { // recursive case


            int midPoint = users.size() / 2;

            UserLocationTask firstHalfSubtask = new UserLocationTask(users.subList(0, midPoint));

            UserLocationTask secondHalfSubtask = new UserLocationTask(users.subList(midPoint, users.size()));

            //asynchronously work out the left side
            firstHalfSubtask.fork();

            // Work out the right hand side in this thread.
            VisitedLocation secondHalfResult = secondHalfSubtask.compute();

            // Get back the left side when it's ready
            VisitedLocation firstHalResult = firstHalfSubtask.join(); // wait for the first task result

            return firstHalResult;

        }
    }

    private VisitedLocation getUserVisitedLocation() {
        VisitedLocation visitedLocation = null;
        int counter = 0;
        for (User user : users) {
            visitedLocation = gpsUtil.getUserLocation(user.getUserId());
            user.addToVisitedLocations(visitedLocation);
            counter++;
            System.out.println("user #" + counter);
        }
        return visitedLocation;
    }

}


