package com.dtdl.restaurant.service;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.dtdl.restaurant.model.Restaurant;
import com.dtdl.restaurant.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Value;




@Service
public class RestaurantService {

    private RestaurantRepository repository;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private WebClient webClient;

    @PostConstruct
    public void initWebClient() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .build();
    }
    public List<Restaurant> getTopRestaurants(String cuisine, double userLat, double userLon,
                                              double ratingWeight, double distanceWeight,
                                              Integer minRating, Integer maxDistanceKm) {
        return repository.findAll().stream()
                .filter(r -> r.getCuisines().contains(cuisine))
                .filter(r -> minRating == null || r.getRating().getOverallRating() >= minRating)
                .map(r -> {
                    double distance = getDistance(r.getGeoLocation().getCoordinates(), userLat, userLon);
                    double score = computeScore(r.getRating().getOverallRating(), distance, ratingWeight, distanceWeight);
                    return new ScoredRestaurant(r, score, distance);
                })
                .filter(sr -> maxDistanceKm == null || sr.getDistance() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(ScoredRestaurant::getScore).reversed())
                .limit(5)
                .map(ScoredRestaurant::getRestaurant)
                .collect(Collectors.toList());
    }
    public Mono<String> getOpenAIRecommendationExplanation(String restaurantName, String cuisine, double rating, double distanceKm) {
        String prompt = String.format("Explain why '%s' is a good recommendation for %s cuisine with %.1f★ rating and located %.1f km away.",
                restaurantName, cuisine, rating, distanceKm);

        return webClient.post()
                .bodyValue("""
                    {
                      "model": "gpt-3.5-turbo",
                      "messages": [
                        {"role": "system", "content": "You are a helpful assistant."},
                        {"role": "user", "content": "" + prompt + ""}
                      ]
                    }
                """)
                .retrieve()
                .bodyToMono(String.class);
    }
    private double getDistance(List<Double> coordinates, double lat, double lon) {
        double lon1 = coordinates.get(0);
        double lat1 = coordinates.get(1);
        double theta = lon - lon1;
        double dist = Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(lat1)) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        return dist;
    }
    private double computeScore(double rating, double distance, double ratingWeight, double distanceWeight) {
        double normalizedRating = rating / 5.0;
        double normalizedDistance = Math.max(0.0, 1.0 - (distance / 10));
        return (normalizedRating * ratingWeight) + (normalizedDistance * distanceWeight);
    }
}



