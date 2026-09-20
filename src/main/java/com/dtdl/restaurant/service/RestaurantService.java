package com.dtdl.restaurant.service;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.AiRecommendation;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.rag.RecommendationPromptBuilder;
import com.dtdl.restaurant.rag.RestaurantRetriever;
import com.dtdl.restaurant.rag.RuleBasedRecommender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Orchestrates the RAG recommendation flow:
 *   retrieve (pgvector semantic + geo/price/rating filters, distance fallback)
 *   -> build a versioned prompt
 *   -> LLM generation with enforced JSON output
 *   -> match results back to real catalog rows
 *   -> rule-based fallback if the LLM is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private static final int MAX_CANDIDATES = 10;

    private final RestaurantRetriever retriever;
    private final RecommendationPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final RuleBasedRecommender ruleBasedRecommender;

    public List<RecommendationDTO> getTopRestaurantRecommendations(
            UserPreferenceRequestApiModel pref, double userLat, double userLon) {

        RestaurantRetriever.RetrievalResult result =
                retriever.retrieve(pref, userLat, userLon, MAX_CANDIDATES);
        List<RestaurantEntity> candidates = result.restaurants();

        if (candidates.isEmpty()) {
            return List.of();
        }
        log.debug("Retrieved {} candidates (semantic={})", candidates.size(), result.semantic());

        String prompt = promptBuilder.build(pref, candidates, MAX_CANDIDATES);
        List<AiRecommendation> aiRecommendations = geminiClient.generateRecommendations(prompt);
        List<RecommendationDTO> llmResults = matchToCandidates(aiRecommendations, candidates);

        if (!llmResults.isEmpty()) {
            log.debug("Served {} LLM recommendations (prompt={})",
                    llmResults.size(), promptBuilder.getPromptVersion());
            return llmResults;
        }

        log.info("LLM unavailable or empty; serving rule-based fallback for {} candidates.",
                candidates.size());
        return ruleBasedRecommender.recommend(candidates);
    }

    /** Keep only LLM items that map (by name) to a real retrieved candidate; drop hallucinations. */
    private List<RecommendationDTO> matchToCandidates(
            List<AiRecommendation> aiRecommendations, List<RestaurantEntity> candidates) {
        Set<String> seen = new HashSet<>();
        return aiRecommendations.stream()
                .filter(ai -> ai.getName() != null && seen.add(ai.getName().toLowerCase()))
                .map(ai -> candidates.stream()
                        .filter(r -> r.getName() != null && r.getName().equalsIgnoreCase(ai.getName()))
                        .findFirst()
                        .map(match -> RecommendationDTO.builder()
                                .id(match.getId())
                                .restaurantName(match.getName())
                                .justification(ai.getJustification())
                                .distance(match.getDistanceKm() == null ? 0.0 : match.getDistanceKm())
                                .overallRating(match.getOverallRating())
                                .priceRange(match.getPriceRange())
                                .cuisines(match.getCuisines())
                                .latitude(match.getLatitude())
                                .longitude(match.getLongitude())
                                .build())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
