 package com.dtdl.restaurant.service;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 import org.springframework.web.reactive.function.client.WebClient;
 import reactor.core.publisher.Mono;
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

         try {

             Map<String, Object> requestBody = Map.of(
                     "contents", List.of(
                             Map.of(
                                     "parts", List.of(
                                             Map.of("text", prompt)
                                     )
                             )
                     )
             );

             Map<String, Object> response =
                     geminiWebClient.post()
                             .uri(uriBuilder ->
                                     uriBuilder
                                             .queryParam("key", apiKey)
                                             .build()
                             )
                             .bodyValue(requestBody)
                             .retrieve()
                             .bodyToMono(Map.class)
                             .block();

             if (response == null) {
                 return "Recommended based on proximity.";
             }

             List<?> candidates =
                     (List<?>) response.get("candidates");

             if (candidates == null || candidates.isEmpty()) {
                 return "Recommended based on proximity.";
             }

             Map<?, ?> content =
                     (Map<?, ?>)
                             ((Map<?, ?>) candidates.get(0))
                                     .get("content");

             List<?> parts =
                     (List<?>) content.get("parts");

             if (parts == null || parts.isEmpty()) {
                 return "Recommended based on proximity.";
             }

             return (String)
                     ((Map<?, ?>) parts.get(0))
                             .get("text");

         } catch (Exception e) {

             log.error("Gemini failed, using fallback", e);

             return "Recommended based on location and rating.";
         }
     }
 }
