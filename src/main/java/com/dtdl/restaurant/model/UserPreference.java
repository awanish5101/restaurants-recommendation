package com.dtdl.restaurant.model;

import lombok.Data;

@Data
public class UserPreference {
    private String preferredCuisine;
    private double maxDistanceInKm;
    private boolean prioritizeRating;
    private int preferredPriceRange;
    private int minimumRating;
}

