package com.dtdl.restaurant.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.Retry.RetrySignal;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF     = Duration.ofSeconds(20);

    private final WebClient openAiWebClient;

    public String generateExplanation(String prompt) {
        var body = Map.of(
                "model", "gpt-4o",
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role", "system",  "content", "You are a helpful assistant."),
                        Map.of("role", "user",    "content", prompt)
                )
        );

        try {
            JsonNode response = openAiWebClient.post()
                    .bodyValue(body)
                    .retrieve()
                    // if we get a 429, throw a specialized exception to trigger retry
                    .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS,
                            resp -> {
                                String retryAfter = resp.headers()
                                        .header("Retry-After")
                                        .stream()
                                        .findFirst()
                                        .orElse("");
                                long delaySeconds = parseRetryAfter(retryAfter);
                                return Mono.error(new RateLimitException(delaySeconds));
                            }
                    )
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                            .maxBackoff(MAX_BACKOFF)
                            .jitter(0.5)
                            .filter(this::isRateLimitException)
                            .doBeforeRetry(this::logRetry)
                    )
                    .block();

            return response
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (RateLimitException e) {
            log.error("Exceeded rate limit after {} retries – giving up", MAX_RETRIES, e);
            return "Request rate‐limited. Please try again later.";
        } catch (WebClientResponseException e) {
            log.error("OpenAI API returned HTTP {}: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return "OpenAI API error: " + e.getStatusCode();
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI", e);
            return "Could not generate explanation.";
        }
    }

    private boolean isRateLimitException(Throwable t) {
        return t instanceof RateLimitException;
    }

    private void logRetry(RetrySignal signal) {
        log.warn("Rate limit hit: retry #{}, cause: {}",
                signal.totalRetriesInARow(),
                signal.failure().getMessage());
    }

    private long parseRetryAfter(String header) {
        try {
            return Long.parseLong(header);
        } catch (Exception ex) {
            return INITIAL_BACKOFF.getSeconds();
        }
    }

    /**
     * Marker exception to indicate HTTP 429 + optional Retry-After.
     */
    private static class RateLimitException extends RuntimeException {
        RateLimitException(long retryAfterSeconds) {
            super("Rate limited; retry after " + retryAfterSeconds + "s");
        }
    }
}
