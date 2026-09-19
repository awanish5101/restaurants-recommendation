package com.dtdl.restaurant.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires a Spring AI {@link EmbeddingModel} against Gemini's OpenAI-compatible
 * endpoint. Auto-configuration is disabled (spring.ai.model.embedding=none) so we
 * can set the exact base URL + path Gemini expects, which differ from OpenAI's.
 */
@Configuration
public class EmbeddingConfig {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_EMBEDDINGS_PATH = "/v1beta/openai/embeddings";

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${app.embedding.model:gemini-embedding-001}") String model,
            @Value("${app.embedding.dimensions:768}") Integer dimensions) {

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(GEMINI_BASE_URL)
                .embeddingsPath(GEMINI_EMBEDDINGS_PATH)
                // Empty key is tolerated so the app still boots; calls then fail and
                // retrieval degrades to distance-based ranking.
                .apiKey(apiKey == null || apiKey.isBlank() ? "unset" : apiKey)
                .build();

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(dimensions)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }
}
