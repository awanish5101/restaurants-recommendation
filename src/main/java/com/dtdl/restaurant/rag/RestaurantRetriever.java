package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import com.dtdl.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Retrieval stage of the RAG pipeline. Combines semantic similarity (pgvector
 * cosine distance) with structured pre-filters (geo bounding box, price, rating)
 * in a single SQL query, then re-ranks by a blend of semantic relevance and
 * proximity. Degrades to pure distance ranking when embeddings/the query vector
 * are unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantRetriever {

    private static final double KM_PER_DEG_LAT = 111.0;
    private static final double SEMANTIC_WEIGHT = 0.75;
    private static final double PROXIMITY_WEIGHT = 0.25;

    // Haversine distance (km) as a SQL expression, clamped to avoid acos domain errors.
    private static final String DIST_KM_SQL = """
            (6371 * acos(least(1.0, greatest(-1.0,
                cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lon))
              + sin(radians(:lat)) * sin(radians(latitude))))))""";

    private final NamedParameterJdbcTemplate jdbc;
    private final RestaurantRepository restaurantRepository;
    private final EmbeddingService embeddingService;

    /** A retrieval hit: restaurant id, cosine distance (0 when non-semantic), km from query. */
    private record ScoredId(Long id, double cosDist, double distKm) {}

    /** Outcome of a retrieval: the ranked restaurants and whether semantic search was used. */
    public record RetrievalResult(List<RestaurantEntity> restaurants, boolean semantic) {}

    public RetrievalResult retrieve(UserPreferenceRequestApiModel pref,
                                    double lat, double lon, int k) {
        Optional<float[]> queryVector = embeddingService.embedQuery(buildQueryText(pref));
        boolean canSemantic = queryVector.isPresent() && restaurantRepository.countWithEmbedding() > 0;

        List<ScoredId> hits = canSemantic
                ? vectorSearch(queryVector.get(), pref, lat, lon, k * 3)
                : distanceSearch(pref, lat, lon, k * 3);

        boolean semantic = canSemantic;
        if (hits.isEmpty() && canSemantic) {
            log.debug("Semantic search returned nothing; falling back to distance search.");
            hits = distanceSearch(pref, lat, lon, k * 3);
            semantic = false;
        }

        double maxDistance = pref.getMaxDistanceInKm();
        List<ScoredId> ranked = hits.stream()
                .filter(h -> h.distKm() <= maxDistance)
                .sorted(rankComparator(semantic, maxDistance))
                .limit(k)
                .toList();

        return new RetrievalResult(loadInOrder(ranked), semantic);
    }

    private Comparator<ScoredId> rankComparator(boolean semantic, double maxDistance) {
        if (!semantic) {
            return Comparator.comparingDouble(ScoredId::distKm);
        }
        // Higher blended score first: semantic similarity blended with proximity.
        return Comparator.comparingDouble((ScoredId h) -> {
            double similarity = 1.0 - h.cosDist();
            double proximity = maxDistance <= 0 ? 0 : 1.0 - (h.distKm() / maxDistance);
            return SEMANTIC_WEIGHT * similarity + PROXIMITY_WEIGHT * proximity;
        }).reversed();
    }

    private List<ScoredId> vectorSearch(float[] queryVector, UserPreferenceRequestApiModel pref,
                                        double lat, double lon, int limit) {
        String sql = "SELECT id, (embedding <=> CAST(:qvec AS vector)) AS cos_dist, "
                + DIST_KM_SQL + " AS dist_km "
                + "FROM restaurants "
                + "WHERE embedding IS NOT NULL " + geoAndFilterClause()
                + " ORDER BY embedding <=> CAST(:qvec AS vector) LIMIT :k";
        MapSqlParameterSource params = baseParams(pref, lat, lon, limit)
                .addValue("qvec", EmbeddingService.toVectorLiteral(queryVector));
        return jdbc.query(sql, params, (rs, i) ->
                new ScoredId(rs.getLong("id"), rs.getDouble("cos_dist"), rs.getDouble("dist_km")));
    }

    private List<ScoredId> distanceSearch(UserPreferenceRequestApiModel pref,
                                          double lat, double lon, int limit) {
        String sql = "SELECT id, 0.0 AS cos_dist, " + DIST_KM_SQL + " AS dist_km "
                + "FROM restaurants WHERE 1=1 " + geoAndFilterClause()
                + " ORDER BY dist_km LIMIT :k";
        return jdbc.query(sql, baseParams(pref, lat, lon, limit), (rs, i) ->
                new ScoredId(rs.getLong("id"), rs.getDouble("cos_dist"), rs.getDouble("dist_km")));
    }

    private String geoAndFilterClause() {
        return "AND latitude BETWEEN :latMin AND :latMax "
                + "AND longitude BETWEEN :lonMin AND :lonMax "
                + "AND (:minRating <= 0 OR overall_rating >= :minRating) "
                + "AND (:maxPrice < 0 OR price_range <= :maxPrice) ";
    }

    private MapSqlParameterSource baseParams(UserPreferenceRequestApiModel pref,
                                             double lat, double lon, int limit) {
        double latDelta = pref.getMaxDistanceInKm() / KM_PER_DEG_LAT;
        double lonDelta = pref.getMaxDistanceInKm()
                / (KM_PER_DEG_LAT * Math.max(0.01, Math.cos(Math.toRadians(lat))));
        return new MapSqlParameterSource()
                .addValue("lat", lat)
                .addValue("lon", lon)
                .addValue("latMin", lat - latDelta)
                .addValue("latMax", lat + latDelta)
                .addValue("lonMin", lon - lonDelta)
                .addValue("lonMax", lon + lonDelta)
                .addValue("minRating", pref.getMinimumRating())
                .addValue("maxPrice", pref.getPreferredPriceRange() <= 0 ? -1 : pref.getPreferredPriceRange())
                .addValue("k", limit);
    }

    private List<RestaurantEntity> loadInOrder(List<ScoredId> ranked) {
        if (ranked.isEmpty()) return List.of();
        List<Long> ids = ranked.stream().map(ScoredId::id).toList();
        Map<Long, RestaurantEntity> byId = restaurantRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(RestaurantEntity::getId, Function.identity()));
        Map<Long, RestaurantEntity> ordered = new LinkedHashMap<>();
        for (ScoredId hit : ranked) {
            RestaurantEntity e = byId.get(hit.id());
            if (e != null) {
                e.setDistanceKm(hit.distKm());
                ordered.put(e.getId(), e);
            }
        }
        return List.copyOf(ordered.values());
    }

    private String buildQueryText(UserPreferenceRequestApiModel pref) {
        String cuisine = (pref.getPreferredCuisine() == null || pref.getPreferredCuisine().isBlank())
                ? "great" : pref.getPreferredCuisine();
        StringBuilder sb = new StringBuilder("Looking for a ").append(cuisine).append(" restaurant");
        if (pref.getPreferredPriceRange() > 0) {
            sb.append(" around price level ").append(pref.getPreferredPriceRange());
        }
        if (pref.getMinimumRating() > 0) {
            sb.append(" with a rating of at least ").append(pref.getMinimumRating());
        }
        return sb.append('.').toString();
    }
}
