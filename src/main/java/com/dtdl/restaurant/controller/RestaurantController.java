package com.dtdl.restaurant.controller;

import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/recommend")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestBody @Validated UserPreferenceRequestApiModel pref) {

        List<RecommendationDTO> recommendations =
                restaurantService.getTopRestaurantRecommendations(pref, latitude, longitude);

        return ResponseEntity.ok(recommendations);
    }
}
