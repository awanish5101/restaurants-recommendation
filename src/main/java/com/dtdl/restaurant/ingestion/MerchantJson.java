package com.dtdl.restaurant.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Raw shape of a record in {@code data/restaurants.sample.json} (produced by
 * {@code scripts/sample_data.py}). Kept separate from the JPA entity so the
 * ingestion format can evolve independently of the persistence model.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantJson {
    private Long id;
    private String name;
    private String description;
    private List<String> cuisines;
    private Integer priceRange;
    private Double overallRating;
    private Integer numberOfRatings;
    private Double latitude;
    private Double longitude;
    private String neighborhood;
    private String city;
    private String state;
    private String phone;
    private String websiteUrl;
    private List<String> features;
}
