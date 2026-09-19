package com.dtdl.restaurant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "restaurant")
public class Restaurant {

    @Id
    private Long id;

    // menu.name → restaurant name
    @JsonProperty("menu")
    private Menu menu;

    private String description;
    private List<String> cuisines;
    private int priceRange;
    private Rating rating;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("geoLocation")
    private GeoLocation geoLocation;

    private double distance;

    public String getName() {

        if (menu != null && menu.getName() != null) {
            return menu.getName();
        }

        return "Unknown";
    }
}