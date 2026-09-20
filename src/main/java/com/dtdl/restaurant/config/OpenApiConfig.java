package com.dtdl.restaurant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI restaurantOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("AI Restaurant Recommendation API")
                .version("v1")
                .description("""
                        RAG-powered restaurant recommendations: pgvector semantic search over
                        restaurant embeddings + geo/price/rating filters, with LLM-generated
                        justifications and a rule-based fallback when the LLM is unavailable.""")
                .license(new License().name("MIT")));
    }
}
