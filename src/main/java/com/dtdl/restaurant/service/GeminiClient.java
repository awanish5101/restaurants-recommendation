 package com.dtdl.restaurant.service;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 import org.springframework.web.reactive.function.client.WebClient;
 import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.List;
 import java.util.Map;

 @Service
 @RequiredArgsConstructor
 @Slf4j
 public class GeminiClient {

     private final WebClient geminiWebClient;

     @Value("${gemini.api.key}")
     private String apiKey;

     public String generateExplanation(String prompt) {
         Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
         );

        return geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    log.error("Gemini API returned error: {}", response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(body -> log.error("Error body: {}", body))
                            .then(Mono.error(new RuntimeException("Gemini API call failed")));
                })
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .map(body -> {
                    try {
                        List<?> candidates = (List<?>) body.get("candidates");
                        if (candidates != null && !candidates.isEmpty()) {
                            Map<String, Object> content = (Map<String, Object>) ((Map<?, ?>) candidates.get(0)).get("content");
                            List<?> parts = (List<?>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                return (String) ((Map<?, ?>) parts.get(0)).get("text"); // ✅ Only extract raw text
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to extract Gemini response", e);
                    }
                    return "";
                })

                .block();
     }
 }
