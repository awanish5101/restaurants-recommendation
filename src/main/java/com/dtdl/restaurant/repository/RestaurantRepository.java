package com.dtdl.restaurant.repository;

import com.dtdl.restaurant.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {

    /** Ids of restaurants that still need an embedding computed. */
    @Query(value = "SELECT id FROM restaurants WHERE embedding IS NULL", nativeQuery = true)
    List<Long> findIdsWithoutEmbedding();

    @Query(value = "SELECT count(*) FROM restaurants WHERE embedding IS NOT NULL", nativeQuery = true)
    long countWithEmbedding();
}
