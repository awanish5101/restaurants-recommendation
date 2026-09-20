package com.dtdl.restaurant.controller;

import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "RAG-powered restaurant recommendations")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/recommend")
    @Operation(summary = "Recommend restaurants",
            description = "Semantic + geo/price/rating retrieval over pgvector, justified by the LLM "
                    + "(rule-based fallback when the LLM is unavailable).")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @Parameter(description = "Diner latitude", example = "40.7580") @RequestParam double latitude,
            @Parameter(description = "Diner longitude", example = "-73.9855") @RequestParam double longitude,
            @Valid @RequestBody UserPreferenceRequestApiModel pref) {

        List<RecommendationDTO> recommendations =
                restaurantService.getTopRestaurantRecommendations(pref, latitude, longitude);
        return ResponseEntity.ok(recommendations);
    }
}
