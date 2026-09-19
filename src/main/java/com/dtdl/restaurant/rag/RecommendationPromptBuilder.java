package com.dtdl.restaurant.rag;

import com.dtdl.restaurant.entity.RestaurantEntity;
import com.dtdl.restaurant.model.request.UserPreferenceRequestApiModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the recommendation prompt from a versioned template kept as a
 * first-class resource ({@code prompts/recommendation.v1.txt}). Bumping the
 * version = adding a new template file + constant, so prompt changes are
 * reviewable and traceable in git.
 */
@Component
public class RecommendationPromptBuilder {

    public static final String PROMPT_VERSION = "recommendation.v1";

    private static final int MAX_DESCRIPTION_CHARS = 240;

    private final String template;

    public RecommendationPromptBuilder() {
        this.template = loadTemplate("prompts/" + PROMPT_VERSION + ".txt");
    }

    public String getPromptVersion() {
        return PROMPT_VERSION;
    }

    public String build(UserPreferenceRequestApiModel pref, List<RestaurantEntity> candidates, int maxResults) {
        String candidatesBlock = candidates.stream()
                .map(this::renderCandidate)
                .collect(Collectors.joining("\n"));
        return template
                .replace("{{maxResults}}", String.valueOf(maxResults))
                .replace("{{preferredCuisine}}", blankToAny(pref.getPreferredCuisine()))
                .replace("{{minimumRating}}", String.valueOf(pref.getMinimumRating()))
                .replace("{{preferredPriceRange}}", String.valueOf(pref.getPreferredPriceRange()))
                .replace("{{candidates}}", candidatesBlock);
    }

    private String renderCandidate(RestaurantEntity r) {
        double rating = r.getOverallRating() == null ? 0.0 : r.getOverallRating();
        int price = r.getPriceRange() == null ? 0 : r.getPriceRange();
        double dist = r.getDistanceKm() == null ? 0.0 : r.getDistanceKm();
        return String.format("- %s | cuisines: %s | rating: %.1f | price: %d | %.2f km%n  %s",
                r.getName(), r.getCuisines(), rating, price, dist, truncate(r.getDescription()));
    }

    private String truncate(String s) {
        if (s == null) return "";
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= MAX_DESCRIPTION_CHARS ? flat : flat.substring(0, MAX_DESCRIPTION_CHARS) + "…";
    }

    private String blankToAny(String s) {
        return (s == null || s.isBlank()) ? "any" : s;
    }

    private String loadTemplate(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing prompt template: " + path, e);
        }
    }
}
