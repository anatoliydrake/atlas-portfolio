package com.atlas.portfolio.service.external;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@Slf4j
public class FinnhubApiClient {

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubApiClient(
            @Value("${finnhub.api.url}") String apiUrl,
            @Value("${finnhub.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    @RateLimiter(name = "finnhub")
    @Retry(name = "finnhub")
    public BigDecimal fetchQuote(String symbol) {
        log.info("Fetching quote from Finnhub API for symbol: {}", symbol);

        FinnhubQuoteResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", symbol)
                        .queryParam("token", apiKey)
                        .build())
                .retrieve()
                .body(FinnhubQuoteResponse.class);

        BigDecimal currentPrice = response.getCurrentPrice();
        if (currentPrice != null) {
            log.info("Successfully fetched quote for {}: {}", symbol, currentPrice);
            return currentPrice;
        } else {
            log.warn("No quote data available for symbol: {}", symbol);
            throw new IllegalStateException("No quote data available for symbol: " + symbol);
        }
    }
}
