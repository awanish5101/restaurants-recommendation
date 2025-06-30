package com.dtdl.restaurant.service;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.Restaurant;
import com.dtdl.restaurant.model.UserPreference;
import com.dtdl.restaurant.repository.RestaurantRepository;


@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final UserHistoryService historyService;
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;

    public List<RecommendationDTO> getTopRecommendations(UserPreference pref, double userLat, double userLon) {
        return restaurantRepo.findAll().stream()
                // filter by cuisine, rating & price
                .filter(r -> r.getCuisines().stream()
                        .anyMatch(c -> c.equalsIgnoreCase(pref.getPreferredCuisine())))
                .filter(r -> r.getRating().getOverallRating() >= pref.getMinimumRating())
                .filter(r -> r.getPriceRange() <= pref.getPreferredPriceRange())
                // enrich with distance & score
                .map(r -> {
                    double distance = DistanceCalculator.haversine(
                            userLat, userLon,
                            r.getGeoLocation().getLatitude(),
                            r.getGeoLocation().getLongitude());
                    if (distance > pref.getMaxDistanceInKm()) return null;

                    int visits = historyService.getRestaurantHistory(r.getId()).size();
                    double score = computeScore(
                            r.getRating().getOverallRating(),
                            distance,
                            visits,
                            pref.isPrioritizeRating() ? 0.6 : 0.3,
                            pref.isPrioritizeRating() ? 0.2 : 0.4,
                            0.2
                    );
                    return new Scored(r, distance, score);
                })
                .filter(s -> s != null)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(5)
                .map(s -> {
                    String prompt = buildPrompt(s.restaurant(), s.distance(), pref);
                    //String explanation = openAIClient.generateExplanation(prompt);
                    String explanation = geminiClient.generateExplanation(prompt);
                    return new RecommendationDTO(s.restaurant().getName(), explanation, s.score(), s.distance());
                })
                .collect(Collectors.toList());
    }

    private record Scored(Restaurant restaurant, double distance, double score) {
    }

    private double computeScore(double rating, double distance, int visits,
                                double wRating, double wDistance, double wPopularity) {
        double normRating = rating / 5.0;
        double normDistance = Math.max(0, 1 - distance / 10);
        double normPopularity = Math.min(1, visits / 50.0);
        return normRating * wRating + normDistance * wDistance + normPopularity * wPopularity;
    }

    private String buildPrompt(Restaurant r, double distance, UserPreference pref) {
        return String.format("""
                Given the following restaurant and user preferences, generate a 1-line explanation why the restaurant is a top recommendation.
                
                User Preferences:
                - Preferred Cuisine: %s
                - Max Distance: %.1f km
                - Min Rating: %d
                
                Restaurant:
                - Name: %s
                - Cuisine: %s
                - Rating: %.1f stars
                - Distance from user: %.1f km
                - Description: %s
                
                Now generate the explanation.
                """, pref.getPreferredCuisine(), pref.getMaxDistanceInKm(), pref.getMinimumRating(), r.getName(), r.getCuisines(), r.getRating().getOverallRating(), distance, r.getDescription());
    }
}