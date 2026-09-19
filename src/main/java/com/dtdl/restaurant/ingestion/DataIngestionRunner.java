package com.dtdl.restaurant.ingestion;

import com.dtdl.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the restaurant catalog on startup when the table is empty, so
 * {@code docker compose up} yields a working demo with no manual import step.
 * Disable with {@code app.ingestion.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.ingestion.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataIngestionRunner implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final DataIngestionService ingestionService;

    @Override
    public void run(String... args) {
        long existing = restaurantRepository.count();
        if (existing > 0) {
            log.info("Restaurant catalog already populated ({} rows); skipping ingestion.", existing);
            return;
        }
        log.info("Empty restaurant catalog detected; ingesting bundled sample…");
        ingestionService.ingestSample();
    }
}
