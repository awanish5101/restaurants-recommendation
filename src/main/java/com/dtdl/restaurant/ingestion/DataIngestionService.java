package com.dtdl.restaurant.ingestion;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loads the bundled restaurant sample into Postgres. Idempotent: entities carry
 * their source ids, so re-running upserts rather than duplicating.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionService {

    private static final String SAMPLE_RESOURCE = "data/restaurants.sample.json";

    private final RestaurantRepository restaurantRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int ingestSample() {
        List<MerchantJson> merchants = readSample();
        List<RestaurantEntity> entities = merchants.stream()
                .filter(this::isValid)
                .map(this::toEntity)
                .toList();
        restaurantRepository.saveAll(entities);
        log.info("Ingested {} restaurants from {}", entities.size(), SAMPLE_RESOURCE);
        return entities.size();
    }

    private List<MerchantJson> readSample() {
        try (InputStream in = new ClassPathResource(SAMPLE_RESOURCE).getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<MerchantJson>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + SAMPLE_RESOURCE, e);
        }
    }

    private boolean isValid(MerchantJson m) {
        return m.getId() != null
                && m.getName() != null && !m.getName().isBlank()
                && m.getLatitude() != null && m.getLongitude() != null;
    }

    private RestaurantEntity toEntity(MerchantJson m) {
        return RestaurantEntity.builder()
                .id(m.getId())
                .name(m.getName())
                .description(m.getDescription())
                .cuisines(orEmpty(m.getCuisines()))
                .features(orEmpty(m.getFeatures()))
                .priceRange(m.getPriceRange())
                .overallRating(m.getOverallRating())
                .numberOfRatings(m.getNumberOfRatings())
                .latitude(m.getLatitude())
                .longitude(m.getLongitude())
                .neighborhood(m.getNeighborhood())
                .city(m.getCity())
                .state(m.getState())
                .phone(m.getPhone())
                .websiteUrl(m.getWebsiteUrl())
                .build();
    }

    private List<String> orEmpty(List<String> in) {
        if (in == null) return new ArrayList<>();
        return in.stream().filter(Objects::nonNull).toList();
    }
}
