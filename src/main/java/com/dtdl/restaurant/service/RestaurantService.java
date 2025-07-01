package com.dtdl.restaurant.service;

import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.Restaurant;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.repository.RestaurantRepository;


@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserHistoryService userHistoryService;
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;

    public List<RecommendationDTO> getTopRestaurantRecommendations(UserPreferenceRequestApiModel userPreferenceRequest, double userLatitude, double userLongitude) {
        return restaurantRepository.findAll().stream()
                .filter(r -> isCuisineMatch(r, userPreferenceRequest))
                .filter(r -> isRatingAcceptable(r, userPreferenceRequest))
                .filter(r -> isPriceWithinRange(r, userPreferenceRequest))
                .map(r -> scoreAndRank(r, userPreferenceRequest, userLatitude, userLongitude))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(5)
                .map(scored -> toRecommendationDTO(scored, userPreferenceRequest))
                .collect(Collectors.toList());
    }

    private boolean isCuisineMatch(Restaurant r, UserPreferenceRequestApiModel pref) {
        String prefCuisine = pref.getPreferredCuisine();
        if (prefCuisine == null || prefCuisine.isBlank() || r.getCuisines() == null) return false;

        return r.getCuisines().stream()
                .anyMatch(c -> c.trim().equalsIgnoreCase(prefCuisine.trim()));
    }


    private boolean isRatingAcceptable(Restaurant r, UserPreferenceRequestApiModel pref) {
        return r.getRating() != null && r.getRating().getOverallRating() >= pref.getMinimumRating();
    }

    private boolean isPriceWithinRange(Restaurant r, UserPreferenceRequestApiModel pref) {
        return r.getPriceRange() <= pref.getPreferredPriceRange();
    }

    private Optional<Scored> scoreAndRank(Restaurant r, UserPreferenceRequestApiModel pref, double userLat, double userLon) {
        double distance = DistanceCalculator.haversine(
                userLat, userLon,
                r.getGeoLocation().getLatitude(),
                r.getGeoLocation().getLongitude());

        if (distance > pref.getMaxDistanceInKm()) return Optional.empty();

        int visitCount = userHistoryService.getRestaurantHistory(r.getId()).size();

        double score = computeScore(
                r.getRating().getOverallRating(),
                distance,
                visitCount,
                pref.isPrioritizeRating() ? 0.6 : 0.3,
                pref.isPrioritizeRating() ? 0.2 : 0.4,
                0.2
        );

        return Optional.of(new Scored(r, distance, score));
    }

    private RecommendationDTO toRecommendationDTO(Scored s, UserPreferenceRequestApiModel pref) {
        String prompt = buildPrompt(s.restaurant(), s.distance(), pref);
        String explanation = geminiClient.generateExplanation(prompt);
        return new RecommendationDTO(
                s.restaurant().getName(),
                explanation,
                s.score(),
                s.distance());
    }

    private double computeScore(double rating, double distance, int visits,
                                double wRating, double wDistance, double wPopularity) {
        double normRating = rating / 5.0;
        double normDistance = Math.max(0, 1 - distance / 10);
        double normPopularity = Math.min(1, visits / 50.0);
        return normRating * wRating + normDistance * wDistance + normPopularity * wPopularity;
    }

    private String buildPrompt(Restaurant r, double distance, UserPreferenceRequestApiModel pref) {
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

    private record Scored(Restaurant restaurant, double distance, double score) {
    }
}