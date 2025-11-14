package com.atlas.portfolio.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FinnhubService {

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubService(
            @Value("${finnhub.api.url}") String apiUrl,
            @Value("${finnhub.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    @Async
    public CompletableFuture<BigDecimal> fetchStockPrice(String symbol) {
        try {
            log.info("Fetching price for symbol: {}", symbol);

            FinnhubQuoteResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubQuoteResponse.class);

            if (response != null && response.getCurrentPrice() != null) {
                log.info("Successfully fetched price for {}: {}", symbol, response.getCurrentPrice());
                return CompletableFuture.completedFuture(response.getCurrentPrice());
            } else {
                log.warn("No price data available for symbol: {}", symbol);
                return CompletableFuture.completedFuture(null);
            }
        } catch (Exception e) {
            log.error("Error fetching price for symbol {}: {}", symbol, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
}
