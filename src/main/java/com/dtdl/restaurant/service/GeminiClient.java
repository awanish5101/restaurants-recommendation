package com.dtdl.restaurant.service;

import com.dtdl.restaurant.model.AiRecommendation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls Gemini's generateContent API for the recommendation justifications.
 *
 * Two robustness properties the previous version lacked:
 *  - Structured output is enforced at the API level via {@code responseMimeType:
 *    application/json} + a {@code responseSchema}, so the model returns parseable
 *    JSON (no markdown fences, no prose to strip).
 *  - Transient failures (HTTP 429 / 5xx) are retried with exponential backoff + jitter.
 *
 * On any unrecoverable failure it returns an empty list; the caller then serves a
 * deterministic rule-based fallback so the user still gets recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    /** OpenAPI-subset schema Gemini uses to constrain the response to our DTO shape. */
    private static final Map<String, Object> RECOMMENDATION_SCHEMA = Map.of(
            "type", "ARRAY",
            "items", Map.of(
                    "type", "OBJECT",
                    "properties", Map.of(
                            "name", Map.of("type", "STRING"),
                            "justification", Map.of("type", "STRING"),
                            "distance", Map.of("type", "NUMBER")),
                    "required", List.of("name", "justification")));

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${app.llm.temperature:0.4}")
    private double temperature;

    @Value("${app.llm.max-retries:3}")
    private int maxRetries;

    /** @return parsed recommendations, or an empty list if the LLM is unavailable. */
    public List<AiRecommendation> generateRecommendations(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not set; skipping LLM call (rule-based fallback will be used).");
            return List.of();
        }
        try {
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "responseSchema", RECOMMENDATION_SCHEMA,
                            "temperature", temperature));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(20))
                            .jitter(0.5)
                            .filter(this::isRetryable))
                    .block();

            String json = extractText(response);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<List<AiRecommendation>>() {});
        } catch (Exception e) {
            log.error("Gemini generation failed ({}); caller will use rule-based fallback.", e.getMessage());
            return List.of();
        }
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof WebClientResponseException w) {
            int status = w.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) return null;
        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;
        Map<String, Object> content = (Map<String, Object>) ((Map<?, ?>) candidates.get(0)).get("content");
        if (content == null) return null;
        List<?> parts = (List<?>) content.get("parts");
        if (parts == null || parts.isEmpty()) return null;
        Object text = ((Map<?, ?>) parts.get(0)).get("text");
        return text == null ? null : text.toString();
    }
}
