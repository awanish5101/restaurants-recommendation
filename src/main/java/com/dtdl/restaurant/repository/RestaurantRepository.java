package com.dtdl.restaurant.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.dtdl.restaurant.model.Restaurant;

public interface RestaurantRepository extends MongoRepository<Restaurant, Long> {
}
