package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin wrapper over the Spring AI {@link EmbeddingModel}. Produces embeddings for
 * queries and restaurant documents, and formats float vectors as pgvector literals.
 * Query embedding failures return {@link Optional#empty()} so callers can degrade.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Embed a single query string; empty if the model call fails (e.g. no API key).
     *  Cached by query text so repeated identical queries don't re-hit the API. */
    @Cacheable(cacheNames = "queryEmbeddings", unless = "#result == null || #result.isEmpty()")
    public Optional<float[]> embedQuery(String text) {
        try {
            return Optional.of(embeddingModel.embed(text));
        } catch (Exception e) {
            log.warn("Query embedding failed ({}); caller should fall back.", e.getMessage());
            return Optional.empty();
        }
    }

    /** Embed a batch of documents. Propagates failures so batch callers can retry/skip. */
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingModel.embed(texts);
    }

    /**
     * The natural-language document that represents a restaurant for semantic search:
     * name, cuisines, location, notable features, and description.
     */
    public String toDocument(RestaurantEntity r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getName());
        if (r.getCuisines() != null && !r.getCuisines().isEmpty()) {
            sb.append(" — ").append(String.join(", ", r.getCuisines()));
        }
        if (r.getCity() != null) {
            sb.append(" in ").append(r.getCity());
            if (r.getState() != null) sb.append(", ").append(r.getState());
        }
        sb.append(".");
        if (r.getFeatures() != null && !r.getFeatures().isEmpty()) {
            sb.append(" Features: ").append(String.join(", ", r.getFeatures())).append(".");
        }
        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            sb.append(" ").append(r.getDescription());
        }
        return sb.toString();
    }

    /** Format a vector as a pgvector literal: {@code [0.1,0.2,...]}. */
    public static String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
