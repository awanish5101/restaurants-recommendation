package com.dtdl.restaurant.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private final WebClient geminiWebClient;

    public String generateExplanation(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", Collections.singletonList(
                        Map.of("parts", Collections.singletonList(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            JsonNode response = geminiWebClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return "Could not generate explanation.";
        }
    }
}
