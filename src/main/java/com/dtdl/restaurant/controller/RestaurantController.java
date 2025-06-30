package com.dtdl.restaurant.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.UserPreference;
import com.dtdl.restaurant.service.RestaurantService;

@RestController
@RequestMapping("/api/restaurants")
@Validated
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/recommend")
    public List<RecommendationDTO> recommend(
            @RequestBody UserPreference preference,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return restaurantService.getTopRecommendations(preference, latitude, longitude);
    }
}