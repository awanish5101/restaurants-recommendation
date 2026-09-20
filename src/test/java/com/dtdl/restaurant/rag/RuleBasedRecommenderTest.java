package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.RecommendationDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedRecommenderTest {

    private final RuleBasedRecommender recommender = new RuleBasedRecommender();

    @Test
    void producesOneDeterministicRecommendationPerCandidate() {
        RestaurantEntity a = candidate(1L, "Tony's", List.of("Italian"), 4.8, 0.2);
        RestaurantEntity b = candidate(2L, "Sushi Zen", List.of("Sushi"), 4.5, 1.3);

        List<RecommendationDTO> recs = recommender.recommend(List.of(a, b));

        assertThat(recs).hasSize(2);
        assertThat(recs.get(0).getRestaurantName()).isEqualTo("Tony's");
        assertThat(recs.get(0).getJustification())
                .startsWith("Tony's").contains("Italian").contains("4.8/5").contains("0.2 km");
        assertThat(recs.get(0).getDistance()).isEqualTo(0.2);
    }

    @Test
    void handlesMissingRatingGracefully() {
        RestaurantEntity r = candidate(1L, "No Rating Cafe", List.of("Cafe"), 0.0, 0.5);
        r.setOverallRating(null);
        List<RecommendationDTO> recs = recommender.recommend(List.of(r));
        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).getJustification()).doesNotContain("/5");
    }

    private RestaurantEntity candidate(Long id, String name, List<String> cuisines, double rating, double dist) {
        RestaurantEntity r = RestaurantEntity.builder()
                .id(id).name(name).cuisines(cuisines).overallRating(rating)
                .latitude(40.0).longitude(-73.0).build();
        r.setDistanceKm(dist);
        return r;
    }
}
