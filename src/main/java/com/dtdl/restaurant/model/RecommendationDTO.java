package com.dtdl.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendationDTO {
    private String restaurantName;
    private String justification;
    private double distance;

}
