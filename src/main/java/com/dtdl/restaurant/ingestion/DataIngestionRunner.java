package com.dtdl.restaurant.ingestion;

import com.dtdl.restaurant.rag.EmbeddingBackfillService;
import com.dtdl.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * On startup: seed the restaurant catalog when empty, then backfill any missing
 * embeddings. Runs after the web server is up, so the API stays responsive while
 * embeddings compute (retrieval falls back to distance ranking until they exist).
 * Disable seeding with {@code app.ingestion.enabled=false}, embeddings with
 * {@code app.embedding.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.ingestion.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataIngestionRunner implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final DataIngestionService ingestionService;
    private final EmbeddingBackfillService embeddingBackfillService;

    @Value("${app.embedding.enabled:true}")
    private boolean embeddingEnabled;

    @Override
    public void run(String... args) {
        long existing = restaurantRepository.count();
        if (existing > 0) {
            log.info("Restaurant catalog already populated ({} rows); skipping ingestion.", existing);
        } else {
            log.info("Empty restaurant catalog detected; ingesting bundled sample…");
            ingestionService.ingestSample();
        }

        if (embeddingEnabled) {
            embeddingBackfillService.backfill();
        } else {
            log.info("Embedding backfill disabled (app.embedding.enabled=false).");
        }
    }
}
