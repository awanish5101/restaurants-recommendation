package com.dtdl.restaurant.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Bean
    public WebClient geminiWebClient() {

        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);

        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient)
                )
                .build();
    }
}