package com.dtdl.restaurant.entity;

import com.dtdl.restaurant.model.InteractionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A single user interaction (visit or rating) with a restaurant. */
@Entity
@Table(name = "user_history")
@Getter
@Setter
@NoArgsConstructor
public class UserHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType interactionType;

    private Double ratingGiven;

    private LocalDateTime interactionTime;
}
