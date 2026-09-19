package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.RecommendationDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Deterministic fallback used when the LLM is unavailable (no API key, quota
 * exhausted, upstream error). Produces a templated justification from the same
 * retrieved candidates, so the endpoint always returns useful recommendations
 * instead of an empty list. This is the "graceful degradation" path.
 */
@Component
public class RuleBasedRecommender {

    public List<RecommendationDTO> recommend(List<RestaurantEntity> candidates) {
        return candidates.stream()
                .map(r -> new RecommendationDTO(r.getName(), justify(r),
                        r.getDistanceKm() == null ? 0.0 : r.getDistanceKm()))
                .toList();
    }

    private String justify(RestaurantEntity r) {
        StringBuilder sb = new StringBuilder(r.getName());
        if (r.getCuisines() != null && !r.getCuisines().isEmpty()) {
            sb.append(" serves ").append(String.join(", ", r.getCuisines()));
        }
        if (r.getOverallRating() != null && r.getOverallRating() > 0) {
            sb.append(", rated ").append(String.format("%.1f", r.getOverallRating())).append("/5");
        }
        if (r.getDistanceKm() != null) {
            sb.append(", about ").append(String.format("%.1f", r.getDistanceKm())).append(" km away");
        }
        return sb.append('.').toString();
    }
}
