package com.dtdl.restaurant.model.request;

import lombok.Data;

@Data
public class UserPreferenceRequestApiModel {
    private String preferredCuisine;
    private double maxDistanceInKm;
    private boolean prioritizeRating;
    private int preferredPriceRange;
    private int minimumRating;
}

