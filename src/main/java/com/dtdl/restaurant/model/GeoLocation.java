package com.dtdl.restaurant.model;

import lombok.Data;
import java.util.List;

@Data
public class GeoLocation {
    private String type;
    private List<Double> coordinates;

    public double getLatitude() {
        return coordinates.get(1);
    }

    public double getLongitude() {
        return coordinates.get(0);
    }
}
