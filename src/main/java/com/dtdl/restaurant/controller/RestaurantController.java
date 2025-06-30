package com.dtdl.restaurant.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RestaurantController {

    @Autowired
    private RestaurantService service;

    //    @PostMapping("/recommend")
//    public List<Restaurant> recommend(@RequestBody UserPreference pref,
//                                      @RequestParam double lat,
//                                      @RequestParam double lon) {
//        return service.getTopRestaurants(pref, lat, lon);
//    }
    @PostMapping("/recommend")
    public List<RecommendationDTO> recommend(@RequestBody UserPreference pref,
                                             @RequestParam double lat,
                                             @RequestParam double lon) {
        return service.getTopRestaurants(pref, lat, lon);
    }
}
//    @GetMapping("/recommend/explain")
//    public Mono<String> explainRecommendation(@RequestParam String restaurant,
//                                              @RequestParam String cuisine,
//                                              @RequestParam double rating,
//                                              @RequestParam double distanceKm) {
//        return service.getOpenAIRecommendationExplanation(restaurant, cuisine, rating, distanceKm);
//    }
