package com.dtdl.restaurant.repository;

import com.dtdl.restaurant.entity.UserHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistoryEntity, Long> {

    List<UserHistoryEntity> findByUserId(String userId);

    List<UserHistoryEntity> findByRestaurantId(Long restaurantId);
}
