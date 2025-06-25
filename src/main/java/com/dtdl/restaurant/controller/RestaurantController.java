package com.dtdl.restaurant.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtdl.restaurant.model.Restaurant;
import com.dtdl.restaurant.service.RestaurantService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private RestaurantService service;

    @GetMapping("/recommend")
    public List<Restaurant> recommend(@RequestParam String cuisine,
                                      @RequestParam double lat,
                                      @RequestParam double lon,
                                      @RequestParam(defaultValue = "0.5") double ratingWeight,
                                      @RequestParam(defaultValue = "0.5") double distanceWeight,
                                      @RequestParam(required = false) Integer minRating,
                                      @RequestParam(required = false) Integer maxDistanceKm) {
        return service.getTopRestaurants(cuisine, lat, lon, ratingWeight, distanceWeight, minRating, maxDistanceKm);
    }
    @GetMapping("/recommend/explain")
    public Mono<String> explainRecommendation(@RequestParam String restaurant,
                                              @RequestParam String cuisine,
                                              @RequestParam double rating,
                                              @RequestParam double distanceKm) {
        return service.getOpenAIRecommendationExplanation(restaurant, cuisine, rating, distanceKm);
    }
}
