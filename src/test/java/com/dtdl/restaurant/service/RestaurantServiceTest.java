package com.dtdl.restaurant.service;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.AiRecommendation;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.rag.RecommendationPromptBuilder;
import com.dtdl.restaurant.rag.RestaurantRetriever;
import com.dtdl.restaurant.rag.RuleBasedRecommender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestaurantServiceTest {

    private RestaurantRetriever retriever;
    private RecommendationPromptBuilder promptBuilder;
    private GeminiClient geminiClient;
    private RuleBasedRecommender ruleBasedRecommender;
    private RestaurantService service;

    private final UserPreferenceRequestApiModel pref = new UserPreferenceRequestApiModel();

    @BeforeEach
    void setUp() {
        retriever = mock(RestaurantRetriever.class);
        promptBuilder = mock(RecommendationPromptBuilder.class);
        geminiClient = mock(GeminiClient.class);
        ruleBasedRecommender = mock(RuleBasedRecommender.class);
        service = new RestaurantService(retriever, promptBuilder, geminiClient, ruleBasedRecommender);
        when(promptBuilder.build(any(), any(), anyInt())).thenReturn("prompt");
        when(promptBuilder.getPromptVersion()).thenReturn("recommendation.v1");
    }

    @Test
    void returnsEmptyWhenNoCandidates() {
        when(retriever.retrieve(any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(new RestaurantRetriever.RetrievalResult(List.of(), false));

        assertThat(service.getTopRestaurantRecommendations(pref, 40.0, -73.0)).isEmpty();
    }

    @Test
    void usesLlmResultsWhenAvailableAndMapsToRealCandidates() {
        RestaurantEntity tony = candidate("Tony's Di Napoli", 0.2);
        when(retriever.retrieve(any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(new RestaurantRetriever.RetrievalResult(List.of(tony), true));
        when(geminiClient.generateRecommendations(any()))
                .thenReturn(List.of(ai("Tony's Di Napoli", "Great Italian nearby."),
                        ai("Hallucinated Spot", "Not in the list")));

        List<RecommendationDTO> recs = service.getTopRestaurantRecommendations(pref, 40.0, -73.0);

        // Hallucinated name dropped; only the real candidate survives.
        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).getRestaurantName()).isEqualTo("Tony's Di Napoli");
        assertThat(recs.get(0).getJustification()).isEqualTo("Great Italian nearby.");
        assertThat(recs.get(0).getDistance()).isEqualTo(0.2);
    }

    @Test
    void fallsBackToRuleBasedWhenLlmReturnsNothing() {
        RestaurantEntity tony = candidate("Tony's Di Napoli", 0.2);
        when(retriever.retrieve(any(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(new RestaurantRetriever.RetrievalResult(List.of(tony), true));
        when(geminiClient.generateRecommendations(any())).thenReturn(List.of()); // LLM unavailable
        when(ruleBasedRecommender.recommend(any()))
                .thenReturn(List.of(new RecommendationDTO("Tony's Di Napoli", "rule-based", 0.2)));

        List<RecommendationDTO> recs = service.getTopRestaurantRecommendations(pref, 40.0, -73.0);

        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).getJustification()).isEqualTo("rule-based");
    }

    private RestaurantEntity candidate(String name, double dist) {
        RestaurantEntity r = RestaurantEntity.builder()
                .id(1L).name(name).latitude(40.0).longitude(-73.0).build();
        r.setDistanceKm(dist);
        return r;
    }

    private AiRecommendation ai(String name, String justification) {
        AiRecommendation a = new AiRecommendation();
        a.setName(name);
        a.setJustification(justification);
        return a;
    }
}
