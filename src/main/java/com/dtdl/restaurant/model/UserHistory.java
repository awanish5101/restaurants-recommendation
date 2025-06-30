package com.dtdl.restaurant.model;

import lombok.Data;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user_history")
public class UserHistory {

    @Id
    private String id;

    private String userId;
    private Long restaurantId;
    private InteractionType interactionType; // VISITED, RATED, etc.
    private Double ratingGiven; // optional for "RATED"
    private LocalDateTime interactionTime;
}
