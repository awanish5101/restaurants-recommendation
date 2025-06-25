package com.dtdl.restaurant.model;

import lombok.Data;

@Data
public class Location {
    private String neighborhood;
    private double latitude;
    private double longitude;
    private Address address;
}
