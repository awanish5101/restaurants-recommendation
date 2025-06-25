package com.dtdl.restaurant.service;

import com.dtdl.restaurant.model.Restaurant;

public class ScoredRestaurant {
    private Restaurant restaurant;
    private double score;
    private double distance;

    public ScoredRestaurant(Restaurant restaurant, double score, double distance) {
        this.restaurant = restaurant;
        this.score = score;
        this.distance = distance;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public double getScore() {
        return score;
    }

    public double getDistance() {
        return distance;
    }
}
