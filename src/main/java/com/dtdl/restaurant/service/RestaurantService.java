package com.dtdl.restaurant.service;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.AiRecommendation;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.rag.RestaurantRetriever;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the RAG recommendation flow: retrieve candidate restaurants
 * (pgvector semantic search + geo/price/rating filters, with a distance-based
 * fallback) and ask the LLM to justify them. Prompt handling and structured
 * output are hardened in a later phase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private static final int MAX_CANDIDATES = 10;

    private final RestaurantRetriever retriever;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public List<RecommendationDTO> getTopRestaurantRecommendations(
            UserPreferenceRequestApiModel pref, double userLat, double userLon) {

        RestaurantRetriever.RetrievalResult result =
                retriever.retrieve(pref, userLat, userLon, MAX_CANDIDATES);
        List<RestaurantEntity> candidates = result.restaurants();

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("Retrieved {} candidates (semantic={})", candidates.size(), result.semantic());

        String prompt = buildPrompt(pref, candidates, userLat, userLon);
        String jsonResponse = geminiClient.generateExplanation(prompt);
        return parseAiRecommendations(jsonResponse, candidates);
    }

    private String buildPrompt(UserPreferenceRequestApiModel pref,
                               List<RestaurantEntity> restaurants, double userLat, double userLon) {
        StringBuilder sb = new StringBuilder("""
                You are an expert restaurant recommendation AI.

                Return exactly %d unique restaurant recommendations as a JSON array — no extra
                text, comments, or markdown.

                Format:
                [
                  {
                    "name": "Restaurant Name",
                    "justification": "A compelling, natural, persuasive explanation (max 2 sentences).",
                    "distance": float (kilometers — use exactly the provided value)
                  }
                ]

                Each 'justification' should start with the restaurant name and weave in its cuisine,
                rating, and how close it is, in engaging language.

                User preferences:
                - Preferred Cuisine: %s
                - Minimum Rating: %d
                - Preferred Price Range: %d
                - User Location: (lat: %.6f, lon: %.6f)

                Restaurant list:
                """.formatted(restaurants.size(), pref.getPreferredCuisine(),
                pref.getMinimumRating(), pref.getPreferredPriceRange(), userLat, userLon));

        for (RestaurantEntity r : restaurants) {
            sb.append(String.format("""
                    - Name: %s
                      Cuisines: %s
                      Rating: %.1f
                      Price Range: %d
                      Distance: %.2f km
                      Description: %s
                    """,
                    r.getName(),
                    r.getCuisines(),
                    r.getOverallRating() == null ? 0.0 : r.getOverallRating(),
                    r.getPriceRange() == null ? 0 : r.getPriceRange(),
                    r.getDistanceKm(),
                    r.getDescription()));
        }
        return sb.toString();
    }

    private List<RecommendationDTO> parseAiRecommendations(String jsonText, List<RestaurantEntity> context) {
        try {
            log.debug("Raw Gemini response: {}", jsonText);
            jsonText = stripCodeFences(jsonText.trim());

            List<AiRecommendation> aiList = objectMapper.readValue(jsonText, new TypeReference<>() {});
            Set<String> seen = new HashSet<>();

            return aiList.stream()
                    .filter(ai -> ai.getName() != null && seen.add(ai.getName().toLowerCase()))
                    .map(ai -> context.stream()
                            .filter(r -> r.getName() != null && r.getName().equalsIgnoreCase(ai.getName()))
                            .findFirst()
                            .map(match -> new RecommendationDTO(
                                    match.getName(), ai.getJustification(), match.getDistanceKm()))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            return Collections.emptyList();
        }
    }

    private String stripCodeFences(String text) {
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
