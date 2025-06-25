package com.dtdl.restaurant.model;

import lombok.Data;

@Data
public class UserPreference {
    private String preferredCuisine;
    private double maxDistanceKm;
    private boolean prioritizeRating;
}

