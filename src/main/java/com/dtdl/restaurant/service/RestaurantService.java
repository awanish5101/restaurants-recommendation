package com.dtdl.restaurant.service;

import jakarta.annotation.PostConstruct;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.dtdl.restaurant.model.RecommendationDTO;
import com.dtdl.restaurant.model.Restaurant;
import com.dtdl.restaurant.model.UserPreference;
import com.dtdl.restaurant.repository.RestaurantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
    @Autowired
    private RestaurantRepository repository;

    @Autowired
    private UserHistoryService userHistoryService;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private WebClient webClient;

    @PostConstruct
    public void initWebClient() {
        this.webClient = WebClient.builder().baseUrl("https://api.openai.com/v1/chat/completions").defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    public List<RecommendationDTO> getTopRestaurants(UserPreference pref, double userLat, double userLon) {

        List<RecommendationDTO> recommendations =  repository.findAll().stream()
                // User Preferences is filtering here(Code Validation)
                .filter(r -> r.getCuisines() != null &&
                                                 r.getCuisines().stream()
                                              .anyMatch(c -> c.equalsIgnoreCase(pref.getPreferredCuisine())))
                .filter(r -> r.getRating().getOverallRating() >= pref.getMinRating())
                //Price-based filtering .
                .filter(r -> r.getPriceRange() <= pref.getPreferredPriceRange())

                //Proximity(Accurate distance logic and filtering applied.)
                .map(r -> {
                    double distance = getDistance(r.getGeoLocation().getCoordinates(), userLat, userLon);
                    if (distance > pref.getMaxDistanceKm())
                        return null;

                    //Popularity calculated using visit history:
                    int visitCount = userHistoryService.getRestaurantHistory(r.getId()).size();

                    //Rating ka normalized value score calculation mein use ho raha hai(Recommendation)
                    double score = computeScore(
                            r.getRating().getOverallRating(), //  Rating
                            distance,//Distance from user
                            visitCount,//Popularity(Kitne users ne is restaurant ko visit/rate kiya hai)
                            pref.isPrioritizeRating() ? 0.6 : 0.3,//Rating ka weight
                            pref.isPrioritizeRating() ? 0.2 : 0.4, //Distance ka weight
                            0.2); //Popularity ka weight

                    return new ScoredRestaurant(r, score, distance);

                })
                .filter(sr -> sr != null)
                .sorted(Comparator.comparingDouble(ScoredRestaurant::getScore).reversed())
                .limit(5)
                .map(sr -> generateExplanation(sr, pref))
                .collect(Collectors.toList());
        return recommendations;
    }

//     Helper Method 1
//    OpenAI-based explanation real-time me kaam kare
//    Transparent Justification(Explanation returned in RecommendationDTO)
    private RecommendationDTO generateExplanation(ScoredRestaurant sr, UserPreference pref) {
        Restaurant r = sr.getRestaurant();
        String prompt = buildPrompt(r, sr.getDistance(), pref);
        String justification = getOpenAIExplanation(prompt);
        return new RecommendationDTO(r.getName(), justification, sr.getScore(), sr.getDistance());
    }


//    private RecommendationDTO generateExplanation(ScoredRestaurant sr, UserPreference pref) {
//        Restaurant r = sr.getRestaurant();
//        String justification = String.format(
//                "#1 match for your '%s' preference, %.1f★ rating, and %.1f km away.",
//                pref.getPreferredCuisine(),
//                r.getRating().getOverallRating(),
//                sr.getDistance()
//        );
//        return new RecommendationDTO(r.getName(), justification, sr.getScore(), sr.getDistance());
//    }


    // Helper Method 2
    private String buildPrompt(Restaurant r, double distance, UserPreference pref) {
        return String.format("""
                Given the following restaurant and user preferences, generate a 1-line explanation why the restaurant is a top recommendation.
                
                User Preferences:
                - Preferred Cuisine: %s
                - Max Distance: %.1f km
                - Min Rating: %d
                
                Restaurant:
                - Name: %s
                - Cuisine: %s
                - Rating: %.1f stars
                - Distance from user: %.1f km
                - Description: %s
                
                Now generate the explanation.
                """, pref.getPreferredCuisine(), pref.getMaxDistanceKm(), pref.getMinRating(), r.getName(), r.getCuisines(), r.getRating().getOverallRating(), distance, r.getDescription());
    }

    // Helper Method 3
    public String getOpenAIExplanation(String prompt) {
        initWebClient();

        Map<String, Object> body = Map.of(
                "model",       "gpt-3.5-turbo",
                "temperature", 0.7,
                "messages", List.of(
                        Map.of("role",    "system", "content", "You are a helpful assistant."),
                        Map.of("role",    "user",   "content", prompt)
                )
        );

        try {
            log.info("Sending request to OpenAI API...");

            JsonNode response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()                               // throws on non-2xx
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            // only retry if it’s a 429 from OpenAI
                            .filter(throwable ->
                                    throwable instanceof WebClientResponseException &&
                                            ((WebClientResponseException) throwable)
                                                    .getStatusCode() == HttpStatus.TOO_MANY_REQUESTS
                            )
                            .doBeforeRetry(retrySignal ->
                                    log.warn("Received 429, retry #{}, next in {}s",
                                            retrySignal.totalRetriesInARow(),
                                            retrySignal.totalRetriesInARow() * 2)
                            )
                    )
                    .block();

            log.info("Response received from OpenAI.");
            return response.at("/choices/0/message/content").asText();

        } catch (WebClientResponseException e) {
            // now you’ll see status + body
            log.error("OpenAI API error: {} — {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());
            return "OpenAI API error: " + e.getStatusCode();

        } catch (Exception ex) {
            log.error("Error while calling OpenAI API", ex);
            return "Could not generate explanation due to an internal error.";
        }
    }

    private double getDistance(List<Double> coordinates, double lat, double lon) {
        double lon1 = coordinates.get(0);
        double lat1 = coordinates.get(1);
        double theta = lon - lon1;
        double dist = Math.sin(Math.toRadians(lat)) * Math.sin(Math.toRadians(lat1)) + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.609344;
        return dist;
    }

    //Rating ko score mein proper weight diya gaya hai.
    private double computeScore(double rating, double distance, int visitCount, double ratingWeight, double distanceWeight, double popularityWeight) {
        double normalizedRating = rating / 5.0;
        double normalizedDistance = Math.max(0.0, 1.0 - (distance / 10));
        double normalizedPopularity = Math.min(1.0, visitCount / 50.0); // 50+ visits = max score

        return (normalizedRating * ratingWeight) + (normalizedDistance * distanceWeight) + (normalizedPopularity * popularityWeight);
    }
}

//Restaurant	Rating (out of 5)	Distance (km)	Visit Count. 	Final Score
//A	                  4.5	           2.0	               20	       0.78  Preffered
//B	                  3.9	           0.5              	10	       0.74
//C	                  4.8	           5.0	                5	       0.71
//
//  Top 1 = Restaurant A because it balances all 3 factors best.



