package com.dtdl.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RestaurantResponse {

    private Restaurant restaurant;
    private String aiExplanation;
}
