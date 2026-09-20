package com.dtdl.restaurant.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Diner preferences that drive the recommendation.")
public class UserPreferenceRequestApiModel {

    @Schema(description = "Preferred cuisine or vibe (used for semantic search).", example = "cozy Italian pasta")
    private String preferredCuisine;

    @Positive
    @Max(500)
    @Schema(description = "Maximum distance from the diner, in kilometres.", example = "10")
    private double maxDistanceInKm;

    @Schema(description = "Whether to weight rating more heavily.", example = "true")
    private boolean prioritizeRating;

    @Min(0)
    @Max(4)
    @Schema(description = "Preferred price level: 0=any, 1=$ … 4=$$$$.", example = "2")
    private int preferredPriceRange;

    @Min(0)
    @Max(5)
    @Schema(description = "Minimum acceptable rating (0 = no minimum).", example = "4")
    private int minimumRating;
}
