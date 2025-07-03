package com.dtdl.restaurant.model;

import lombok.Data;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "restaurants")
public class Restaurant {
    @Id
    private Long id;
    private String name;
    private String description;
    private List<String> cuisines;
    private int priceRange;
    private Rating rating;
    private Location location;
    private GeoLocation geoLocation;
    private List<Hour> hours;
    private List<Benefit> benefits;
    private String websiteUrl;
    private String facebookUrl;
    private String twitterUrl;
    private double distance;
//    private long id;
//    private String name;
//    private String description;
//    private List<String> cuisines;
//    private Rating rating;
//    private double priceRange;
//    private Location location;
//    private List<String> photos;
}
