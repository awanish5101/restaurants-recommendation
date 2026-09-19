package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.repository.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes and stores embeddings for restaurants that don't have one yet.
 * Runs in batches and is fully resumable: it only touches rows where
 * {@code embedding IS NULL}, so a rate-limit or a missing API key simply leaves
 * the remainder for the next run — retrieval degrades to distance-based ranking
 * in the meantime.
 */
@Service
@Slf4j
public class EmbeddingBackfillService {

    private final RestaurantRepository restaurantRepository;
    private final EmbeddingService embeddingService;
    private final NamedParameterJdbcTemplate jdbc;
    private final int batchSize;

    public EmbeddingBackfillService(RestaurantRepository restaurantRepository,
                                    EmbeddingService embeddingService,
                                    NamedParameterJdbcTemplate jdbc,
                                    @Value("${app.embedding.batch-size:50}") int batchSize) {
        this.restaurantRepository = restaurantRepository;
        this.embeddingService = embeddingService;
        this.jdbc = jdbc;
        this.batchSize = batchSize;
    }

    /** @return number of rows embedded in this run. */
    public int backfill() {
        List<Long> pending = restaurantRepository.findIdsWithoutEmbedding();
        if (pending.isEmpty()) {
            log.info("All restaurants already embedded.");
            return 0;
        }
        log.info("Backfilling embeddings for {} restaurants (batch size {})…", pending.size(), batchSize);

        int embedded = 0;
        for (int i = 0; i < pending.size(); i += batchSize) {
            List<Long> batchIds = pending.subList(i, Math.min(i + batchSize, pending.size()));
            List<RestaurantEntity> rows = restaurantRepository.findAllById(batchIds);
            List<String> docs = rows.stream().map(embeddingService::toDocument).toList();
            try {
                List<float[]> vectors = embeddingService.embedBatch(docs);
                persist(rows, vectors);
                embedded += rows.size();
                log.info("Embedded {}/{}", embedded, pending.size());
            } catch (Exception e) {
                log.warn("Embedding batch failed at offset {} ({}). Stopping; will resume next run.",
                        i, e.getMessage());
                break;
            }
        }
        log.info("Embedding backfill done: {} embedded this run.", embedded);
        return embedded;
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
