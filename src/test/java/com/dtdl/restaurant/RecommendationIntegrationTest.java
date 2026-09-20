package com.dtdl.restaurant;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test over a real Postgres+pgvector: Flyway schema, JPA persistence,
 * the retriever's distance path, the controller, and validation - with embeddings
 * and the LLM disabled, so it exercises the rule-based path with no external calls.
 * Runs locally against localhost:5432 and in CI against the pgvector service container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RecommendationIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @BeforeEach
    void seed() {
        restaurantRepository.deleteAll();
        restaurantRepository.saveAll(List.of(
                restaurant(1L, "Tony's Di Napoli", List.of("Italian"), 4.8, 40.7590, -73.9845),
                restaurant(2L, "Sushi Zen", List.of("Sushi"), 4.5, 40.7600, -73.9850),
                restaurant(3L, "Faraway Diner", List.of("American"), 4.0, 34.0522, -118.2437) // Los Angeles
        ));
    }

    @Test
    void recommendReturnsNearbyAndExcludesFarByDistance() {
        Map<String, Object> body = Map.of(
                "preferredCuisine", "Italian", "maxDistanceInKm", 5,
                "prioritizeRating", true, "preferredPriceRange", 0, "minimumRating", 0);

        ResponseEntity<RecommendationDTO[]> resp = rest.postForEntity(
                "/api/restaurants/recommend?latitude=40.7580&longitude=-73.9855",
                body, RecommendationDTO[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
        List<String> names = Arrays.stream(resp.getBody()).map(RecommendationDTO::getRestaurantName).toList();
        assertThat(names).contains("Tony's Di Napoli");
        assertThat(names).doesNotContain("Faraway Diner"); // 3900+ km away, filtered out
    }

    @Test
    void invalidRequestReturns400() {
        Map<String, Object> bad = Map.of("preferredCuisine", "Italian", "maxDistanceInKm", -1);
        ResponseEntity<String> resp = rest.postForEntity(
                "/api/restaurants/recommend?latitude=40.75&longitude=-73.98", bad, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void openApiDocsAreServed() {
        ResponseEntity<String> resp = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("/api/restaurants/recommend");
    }

    private RestaurantEntity restaurant(Long id, String name, List<String> cuisines,
                                        double rating, double lat, double lon) {
        return RestaurantEntity.builder()
                .id(id).name(name).cuisines(cuisines).overallRating(rating)
                .priceRange(2).numberOfRatings(100)
                .latitude(lat).longitude(lon).city("Test City").state("NY")
                .description(name + " description").build();
    }
}
