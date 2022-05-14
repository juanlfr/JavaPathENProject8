package tourGuide.DTO;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import tourGuide.user.User;

import java.util.List;
import java.util.Objects;

public class AttractionDTO {

    private String attractionName;
    private Location userLocation;
    private Location attractionLocation;
    private double distance;
    private int rewardPoints;

    public AttractionDTO(String attractionName, Location userLocation, Location attractionLocation, double distance, int rewardPoints) {
        this.attractionName = attractionName;
        this.userLocation = userLocation;
        this.attractionLocation = attractionLocation;
        this.distance = distance;
        this.rewardPoints = rewardPoints;
    }

    public AttractionDTO() {

    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public Location getAttractionLocation() {
        return attractionLocation;
    }

    public void setAttractionLocation(Location attractionLocation) {
        this.attractionLocation = attractionLocation;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttractionDTO)) return false;
        AttractionDTO that = (AttractionDTO) o;
        return Double.compare(that.getDistance(), getDistance()) == 0 && getRewardPoints() == that.getRewardPoints() && Objects.equals(getAttractionName(), that.getAttractionName()) && Objects.equals(getUserLocation(), that.getUserLocation()) && Objects.equals(getAttractionLocation(), that.getAttractionLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAttractionName(), getUserLocation(), getAttractionLocation(), getDistance(), getRewardPoints());
    }

    @Override
    public String toString() {
        return "AttractionDTO{" +
                "attractionName='" + attractionName + '\'' +
                ", userLocation=" + userLocation +
                ", attractionLocation=" + attractionLocation +
                ", distance=" + distance +
                ", rewardPoints=" + rewardPoints +
                '}';
    }
}
