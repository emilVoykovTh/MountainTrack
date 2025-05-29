package com.example.mountaintrack;

import java.io.Serializable;

public class Trail implements Serializable {
    private int id;
    private String name;
    private String description;
    private boolean isUserCreated;
    private boolean isFavorite;
    private String duration;
    private float distance;
    private double totalAscent;
    private double totalDescent;

    public Trail(int id, String name, String description, boolean isUserCreated, boolean isFavorite,
                 String duration, float distance, double totalAscent, double totalDescent) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isUserCreated = isUserCreated;
        this.isFavorite = isFavorite;
        this.duration = duration;
        this.distance = distance;
        this.totalAscent = totalAscent;
        this.totalDescent = totalDescent;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getDuration() {
        return duration;
    }

    public float getDistance() {
        return distance;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean isUserCreated() {
        return isUserCreated;
    }

    public double getTotalAscent() {
        return totalAscent;
    }

    public double getTotalDescent() {
        return totalDescent;
    }
}
