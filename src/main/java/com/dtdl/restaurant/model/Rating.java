package com.dtdl.restaurant.model;

import lombok.Data;

@Data
public class Rating {
    private double overallRating;
    private int numberOfRatings;
    private int numberOfReviews;
}
