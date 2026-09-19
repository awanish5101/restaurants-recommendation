package com.dtdl.restaurant.model;

import lombok.Data;
import java.util.List;

@Data
public class GeoLocation {

    private String type;

    private List<Double> coordinates;

    public double getLatitude() {
        if (coordinates == null || coordinates.size() < 2)
            return 0;

        return coordinates.get(1); // latitude
    }

    public double getLongitude() {
        if (coordinates == null || coordinates.size() < 2)
            return 0;

        return coordinates.get(0); // longitude
    }
}