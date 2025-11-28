package com.atlas.portfolio.service.external;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

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

    @RateLimiter(name = "finnhub")
    @Retry(name = "finnhub")
    public BigDecimal fetchStockPriceSync(String symbol) {
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
            return response.getCurrentPrice();
        } else {
            log.warn("No price data available for symbol: {}", symbol);
            throw new IllegalStateException("No price data available for symbol: " + symbol);
        }
    }
}
