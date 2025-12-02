package com.atlas.portfolio.service;

import com.atlas.portfolio.service.external.ExchangeRateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final String BASE_CURRENCY = "USD";
    private static final int RATE_CALCULATION_SCALE = 10;
    private static final int MONEY_DISPLAY_SCALE = 2;

    private final ExchangeRateApiClient exchangeRateApiClient;

    @Cacheable(value = "allExchangeRates", key = "'USD'")
    public Map<String, BigDecimal> getAllRatesFromUSD() {
        log.info("Cache miss - fetching all exchange rates via API client");

        try {
            Map<String, BigDecimal> rates = exchangeRateApiClient.fetchAllRatesFromUSD();
            log.info("Fetched and cached {} exchange rates", rates.size());
            return rates;

        } catch (Exception e) {
            log.error("Error fetching exchange rates: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch exchange rates", e);
        }
    }

    public BigDecimal getRateFromUSD(String targetCurrency) {
        if (BASE_CURRENCY.equals(targetCurrency)) {
            return BigDecimal.ONE;
        }

        Map<String, BigDecimal> rates = getAllRatesFromUSD();
        BigDecimal rate = rates.get(targetCurrency);

        if (rate == null) {
            throw new RuntimeException("Currency not supported: " + targetCurrency);
        }

        return rate;
    }

    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = getRateFromUSD(fromCurrency);
        BigDecimal toRate = getRateFromUSD(toCurrency);

        return toRate.divide(fromRate, RATE_CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(MONEY_DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @CacheEvict(value = "allExchangeRates", allEntries = true)
    public void refreshExchangeRateCache() {
        log.info("Starting scheduled exchange rate cache refresh");

        try {
            Map<String, BigDecimal> rates = getAllRatesFromUSD();
            log.info("Exchange rate cache refreshed successfully with {} currencies", rates.size());

        } catch (Exception e) {
            log.error("Failed to refresh exchange rate cache: {}", e.getMessage(), e);
        }
    }

    @CacheEvict(value = "allExchangeRates", allEntries = true)
    public void clearCache() {
        log.info("Exchange rate cache cleared manually");
    }

    public Map<String, BigDecimal> getAllSupportedCurrencies() {
        return getAllRatesFromUSD();
    }
}
