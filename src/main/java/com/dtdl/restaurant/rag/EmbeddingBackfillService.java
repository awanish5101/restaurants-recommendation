package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.repository.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes and stores embeddings for restaurants that don't have one yet.
 *
 * Runs asynchronously so it never blocks startup, and is fully resumable: it only
 * touches rows where {@code embedding IS NULL}. Each batch is retried with
 * exponential backoff on failure (e.g. free-tier 429s), which also paces the load;
 * anything still missing is simply picked up on the next run. Until a row is
 * embedded, retrieval degrades to distance-based ranking for it.
 */
@Service
@Slf4j
public class EmbeddingBackfillService {

    private final RestaurantRepository restaurantRepository;
    private final EmbeddingService embeddingService;
    private final NamedParameterJdbcTemplate jdbc;
    private final int batchSize;
    private final int maxRetries;
    private final long backoffMs;

    public EmbeddingBackfillService(RestaurantRepository restaurantRepository,
                                    EmbeddingService embeddingService,
                                    NamedParameterJdbcTemplate jdbc,
                                    @Value("${app.embedding.batch-size:50}") int batchSize,
                                    @Value("${app.embedding.max-retries:4}") int maxRetries,
                                    @Value("${app.embedding.retry-backoff-ms:20000}") long backoffMs) {
        this.restaurantRepository = restaurantRepository;
        this.embeddingService = embeddingService;
        this.jdbc = jdbc;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.backoffMs = backoffMs;
    }

    @Async
    public void backfill() {
        List<Long> pending = restaurantRepository.findIdsWithoutEmbedding();
        if (pending.isEmpty()) {
            log.info("All restaurants already embedded.");
            return;
        }
        log.info("Backfilling embeddings for {} restaurants (batch size {})…", pending.size(), batchSize);

        int embedded = 0;
        for (int i = 0; i < pending.size(); i += batchSize) {
            List<Long> batchIds = pending.subList(i, Math.min(i + batchSize, pending.size()));
            List<RestaurantEntity> rows = restaurantRepository.findAllById(batchIds);
            List<String> docs = rows.stream().map(embeddingService::toDocument).toList();

            List<float[]> vectors = embedWithRetry(docs, i);
            if (vectors == null) {
                log.warn("Giving up embedding backfill at offset {}; {} remain. Will resume next run.",
                        i, pending.size() - embedded);
                break;
            }
            persist(rows, vectors);
            embedded += rows.size();
            log.info("Embedded {}/{}", embedded, pending.size());
        }
        log.info("Embedding backfill run complete: {} embedded.", embedded);
    }

    /** Embed a batch, retrying transient failures with exponential backoff. Null if exhausted. */
    private List<float[]> embedWithRetry(List<String> docs, int offset) {
        long wait = backoffMs;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return embeddingService.embedBatch(docs);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.warn("Batch at offset {} failed after {} attempts: {}", offset, maxRetries, e.getMessage());
                    return null;
                }
                log.info("Batch at offset {} failed (attempt {}/{}): {}. Backing off {}ms…",
                        offset, attempt, maxRetries, e.getMessage(), wait);
                if (!sleep(wait)) return null;
                wait *= 2;
            }
        }
        return null;
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void persist(List<RestaurantEntity> rows, List<float[]> vectors) {
        MapSqlParameterSource[] params = new MapSqlParameterSource[rows.size()];
        for (int j = 0; j < rows.size(); j++) {
            params[j] = new MapSqlParameterSource()
                    .addValue("id", rows.get(j).getId())
                    .addValue("vec", EmbeddingService.toVectorLiteral(vectors.get(j)));
        }
        jdbc.batchUpdate("UPDATE restaurants SET embedding = CAST(:vec AS vector) WHERE id = :id", params);
    }
}
