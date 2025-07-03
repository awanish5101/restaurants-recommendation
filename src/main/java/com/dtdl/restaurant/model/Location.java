package com.dtdl.restaurant.model;

import lombok.Data;

@Data
public class Location {
    private String neighborhood;
    private double lat;
    private double lon;
    private Address address;
}
