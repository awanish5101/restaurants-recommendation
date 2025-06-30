package com.dtdl.restaurant.service;

import com.dtdl.restaurant.model.UserHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dtdl.restaurant.repository.UserHistoryRepository;


@Service
public class UserHistoryService {

    @Autowired
    private UserHistoryRepository userHistoryRepository;

    public List<UserHistory> getRestaurantHistory(Long restaurantId) {
        return userHistoryRepository.findByRestaurantId(restaurantId);
    }

    public void logVisit(String userId, Long restaurantId) {
        UserHistory visit = new UserHistory();
        visit.setUserId(userId);
        visit.setRestaurantId(restaurantId);
        visit.setInteractionTime(java.time.LocalDateTime.now());
        visit.setInteractionType(com.dtdl.restaurant.model.InteractionType.VISITED);
        userHistoryRepository.save(visit);
    }

    public void logRating(String userId, Long restaurantId, Double rating) {
        UserHistory history = new UserHistory();
        history.setUserId(userId);
        history.setRestaurantId(restaurantId);
        history.setRatingGiven(rating);
        history.setInteractionTime(java.time.LocalDateTime.now());
        history.setInteractionType(com.dtdl.restaurant.model.InteractionType.RATED);
        userHistoryRepository.save(history);
    }
}
