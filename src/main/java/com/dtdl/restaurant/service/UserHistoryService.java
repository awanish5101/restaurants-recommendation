package com.dtdl.restaurant.service;

import com.dtdl.restaurant.entity.UserHistoryEntity;
import com.dtdl.restaurant.model.InteractionType;
import com.dtdl.restaurant.repository.UserHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserHistoryRepository userHistoryRepository;

    public List<UserHistoryEntity> getRestaurantHistory(Long restaurantId) {
        return userHistoryRepository.findByRestaurantId(restaurantId);
    }

    public void logVisit(String userId, Long restaurantId) {
        UserHistoryEntity visit = new UserHistoryEntity();
        visit.setUserId(userId);
        visit.setRestaurantId(restaurantId);
        visit.setInteractionType(InteractionType.VISITED);
        visit.setInteractionTime(LocalDateTime.now());
        userHistoryRepository.save(visit);
    }

    public void logRating(String userId, Long restaurantId, Double rating) {
        UserHistoryEntity history = new UserHistoryEntity();
        history.setUserId(userId);
        history.setRestaurantId(restaurantId);
        history.setRatingGiven(rating);
        history.setInteractionType(InteractionType.RATED);
        history.setInteractionTime(LocalDateTime.now());
        userHistoryRepository.save(history);
    }
}
