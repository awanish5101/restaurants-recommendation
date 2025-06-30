package com.dtdl.restaurant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtdl.restaurant.service.UserHistoryService;

@RestController
@RequestMapping("/history")
public class UserHistoryController {

    @Autowired
    private UserHistoryService service;

    @PostMapping("/visit")
    public String logVisit(@RequestParam String userId, @RequestParam Long restaurantId) {
        service.logVisit(userId, restaurantId);
        return "Visit logged successfully.";
    }


    @PostMapping("/rate")
    public String logRating(@RequestParam String userId,
                            @RequestParam Long restaurantId,
                            @RequestParam Double rating) {
        service.logRating(userId, restaurantId, rating);
        return "Rating logged successfully.";
    }
}
