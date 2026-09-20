package com.dtdl.restaurant.controller;

import com.dtdl.restaurant.model.ApiMessage;
import com.dtdl.restaurant.service.UserHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Tag(name = "User history", description = "Log diner interactions with restaurants")
public class UserHistoryController {

    private final UserHistoryService service;

    @PostMapping("/visit")
    @Operation(summary = "Log a restaurant visit")
    public ResponseEntity<ApiMessage> logVisit(@RequestParam String userId, @RequestParam Long restaurantId) {
        service.logVisit(userId, restaurantId);
        return ResponseEntity.ok(ApiMessage.ok("Visit logged."));
    }

    @PostMapping("/rate")
    @Operation(summary = "Log a restaurant rating")
    public ResponseEntity<ApiMessage> logRating(@RequestParam String userId,
                                                @RequestParam Long restaurantId,
                                                @RequestParam Double rating) {
        service.logRating(userId, restaurantId, rating);
        return ResponseEntity.ok(ApiMessage.ok("Rating logged."));
    }
}
