package com.dtdl.restaurant.repository;

import com.dtdl.restaurant.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantRepository extends MongoRepository<Restaurant, Long> {
}
