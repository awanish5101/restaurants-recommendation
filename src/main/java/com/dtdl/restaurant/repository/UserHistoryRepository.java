package com.dtdl.restaurant.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.dtdl.restaurant.model.UserHistory;

public interface UserHistoryRepository extends MongoRepository<UserHistory, String> {
    List<UserHistory> findByUserId(String userId);
    List<UserHistory> findByRestaurantId(Long restaurantId);
}
