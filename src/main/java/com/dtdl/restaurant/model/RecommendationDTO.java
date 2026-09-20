package com.dtdl.restaurant.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant recommendation with rationale and rich metadata")
public class RecommendationDTO {

    @Schema(description = "Restaurant database identifier", example = "42")
    private Long id;

    @Schema(description = "Restaurant name", example = "Tony's Di Napoli")
    private String restaurantName;

    @Schema(description = "LLM-generated or rule-based justification", example = "Tony's serves Italian, rated 4.8/5, about 0.2 km away.")
    private String justification;

    @Schema(description = "Distance from diner in kilometers", example = "0.25")
    private double distance;

    @Schema(description = "Overall diner rating (0.0 to 5.0)", example = "4.8")
    private Double overallRating;

    @Schema(description = "Price range tier (1 to 4)", example = "2")
    private Integer priceRange;

    @Schema(description = "List of cuisine categories", example = "[\"Italian\", \"Pasta\"]")
    private List<String> cuisines;

    @Schema(description = "Latitude coordinate", example = "40.7590")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "-73.9845")
    private Double longitude;

    public RecommendationDTO(String restaurantName, String justification, double distance) {
        this.restaurantName = restaurantName;
        this.justification = justification;
        this.distance = distance;
    }
}
