package com.atlas.portfolio.service;

import com.atlas.portfolio.service.external.FinnhubApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockPriceService {

    private final FinnhubApiClient finnhubApiClient;

    public BigDecimal fetchStockPrice(String symbol) {
        log.debug("Fetching stock price for symbol: {}", symbol);

        try {
            return finnhubApiClient.fetchQuote(symbol);
        } catch (Exception e) {
            log.error("Failed to fetch stock price for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch stock price for " + symbol, e);
        }
    }
}
