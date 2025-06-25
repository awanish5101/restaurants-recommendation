package com.dtdl.restaurant.model;

import lombok.Data;
import java.util.List;

@Data
public class GeoLocation {
    private String type;
    private List<Double> coordinates; // [longitude, latitude]
}
