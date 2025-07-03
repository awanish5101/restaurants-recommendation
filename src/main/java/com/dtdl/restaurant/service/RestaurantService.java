package com.dtdl.restaurant.service;
import com.dtdl.restaurant.model.AiRecommendation;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.Restaurant;
 import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
 @RequiredArgsConstructor
@Slf4j
public class RestaurantService {

     private final RestaurantRepository restaurantRepository;
     private final UserHistoryService userHistoryService;
     private final GeminiClient geminiClient;
     private final ObjectMapper objectMapper;
    public List<RecommendationDTO> getTopRestaurantRecommendations(UserPreferenceRequestApiModel pref, double userLat, double userLon) {
        List<Restaurant> nearbyRestaurants = restaurantRepository.findAll().stream()
                .peek(r -> {
                    double distance = DistanceCalculator.haversine(
                            userLat, userLon,
                            r.getGeoLocation().getLatitude(),
                            r.getGeoLocation().getLongitude());
                    r.setDistance(distance);
                })
                .filter(r -> r.getDistance() <= 80.46) // ~50 miles
                .collect(Collectors.toList());

        if (nearbyRestaurants.isEmpty()) return Collections.emptyList();

        String prompt = buildPrompt(pref, nearbyRestaurants, userLat, userLon);
        String jsonResponse = geminiClient.generateExplanation(prompt);

        return parseAiRecommendations(jsonResponse, nearbyRestaurants);
    }

    private String buildPrompt(UserPreferenceRequestApiModel pref, List<Restaurant> restaurants, double userLat, double userLon) {
        StringBuilder sb = new StringBuilder("""
                You are an expert restaurant recommendation AI.
                
                Your job is to return exactly 10 unique restaurant recommendations in the following JSON format. Respond with **only** the JSON array — no extra text, comments, or markdown.
                
                Format:
                [
                  {
                    "name": "Restaurant Name",
                    "justification": "A compelling explanation including restaurant name, rating, cuisine, distance, and score, written in a natural and persuasive tone.",
                    "score": 0.0 to 1.0,
                    "distance": float (in kilometers — use exactly the provided value)
                  }
                ]
                
                For each restaurant, the 'justification' should:
                - Start with the restaurant's name.
                - Sound like a recommendation from a real expert.
                - Include: cuisine type, overall rating, how close it is (distance), and score.
                - Use engaging language that impresses the user.
                - Keep it short and attractive — max 2 sentences.
                
                Use the following user preferences:
                - Preferred Cuisine: %s
                - Minimum Rating: %d
                - Preferred Price Range: %d
                - User Location: (lat: %.6f, lon: %.6f)
                
                Here is the restaurant list:
                """.formatted(
                pref.getPreferredCuisine(),
                pref.getMinimumRating(),
                pref.getPreferredPriceRange(),
                userLat,
                userLon
        ));

        for (Restaurant r : restaurants) {
            int visitCount = userHistoryService.getRestaurantHistory(r.getId()).size();
            sb.append(String.format("""
                            - Name: %s
                              Cuisines: %s
                              Rating: %.1f
                              Price Range: %d
                              Distance: %.2f km
                              Coordinates: (lat: %.6f, lon: %.6f)
                              Visit Count: %d
                              Description: %s
                            """,
                    r.getName(),
                    r.getCuisines(),
                    r.getRating().getOverallRating(),
                    r.getPriceRange(),
                    r.getDistance(),
                    r.getGeoLocation().getLatitude(),
                    r.getGeoLocation().getLongitude(),
                    visitCount,
                    r.getDescription()));
        }
        return sb.toString();}
    private List<RecommendationDTO> parseAiRecommendations(String jsonText, List<Restaurant> contextRestaurants) {
        try {
            log.debug("Raw Gemini response: {}", jsonText);
            jsonText = jsonText.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            List<AiRecommendation> aiList = objectMapper.readValue(jsonText, new TypeReference<>() {
            });
            Set<String> seenNames = new HashSet<>();

            return aiList.stream()
                    .filter(ai -> seenNames.add(ai.getName().toLowerCase()))
                    .map(ai -> {
                        Restaurant matched = contextRestaurants.stream()
                                .filter(r -> r.getName().equalsIgnoreCase(ai.getName()))
                                .findFirst()
                                .orElse(null);
                        if (matched == null) return null;

                        return new RecommendationDTO(
                                matched.getName(),
                                ai.getJustification(),
                                ai.getScore(),
                                matched.getDistance()
                        );
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            return Collections.emptyList();
        }
     }
}
