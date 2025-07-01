package com.dtdl.restaurant.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.service.RestaurantService;

@RestController
@RequestMapping("/api/restaurants")
@Validated
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/recommend")
    public List<RecommendationDTO> restaurantRecommendations(@RequestBody UserPreferenceRequestApiModel userPreferenceRequestApiModel,
                                                             @RequestParam double latitude,
                                                             @RequestParam double longitude) {
        return restaurantService.getTopRestaurantRecommendations(userPreferenceRequestApiModel, latitude, longitude);
    }
}