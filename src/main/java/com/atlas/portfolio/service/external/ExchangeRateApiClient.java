package com.atlas.portfolio.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class ExchangeRateApiClient {

    private final RestClient restClient;

    public ExchangeRateApiClient(@Value("${exchangerate.api.url}") String apiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    public Map<String, BigDecimal> fetchAllRatesFromUSD() {
        log.info("Fetching all exchange rates from API with USD as base");

        ExchangeRateApiResponse response = restClient.get()
                .uri("/latest/USD")
                .retrieve()
                .body(ExchangeRateApiResponse.class);

        if (response.getRates() == null) {
            throw new RuntimeException("Failed to fetch exchange rates from API");
        }

        log.info("Fetched {} exchange rates from API", response.getRates().size());
        return response.getRates();
    }
}
