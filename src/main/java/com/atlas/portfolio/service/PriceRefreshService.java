package com.atlas.portfolio.service;

import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.AssetRepository;
import com.atlas.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class PriceRefreshService {

    private static final String DEFAULT_CURRENCY = "USD";

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final StockPriceService stockPriceService;
    private final ExchangeRateService exchangeRateService;
    private final JdbcTemplate jdbcTemplate;
    private final Executor priceRefreshExecutor;

    @Transactional
    public void refreshPortfolioPrices(Long portfolioId, Long userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        List<Asset> assets = assetRepository.findByPortfolioId(portfolioId);

        if (assets.isEmpty()) {
            log.info("No assets found in portfolio {}", portfolioId);
            return;
        }

        log.info("Refreshing prices for {} assets in portfolio {}", assets.size(), portfolioId);

        List<Object[]> batchArgs = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = assets.stream()
                .map(asset -> this.refreshAssetPrice(asset, batchArgs))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (!batchArgs.isEmpty()) {
            String sql = "UPDATE assets SET current_price = ?, price_updated_at = NOW() WHERE id = ?";
            int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
            log.info("Bulk updated {} asset prices", updateCounts.length);
        }

        log.info("Finished refreshing prices for portfolio {}", portfolioId);
    }

    private CompletableFuture<Void> refreshAssetPrice(Asset asset, List<Object[]> batchArgs) {
        return switch (asset.getAssetType()) {
            case STOCK -> fetchStockPrice(asset, batchArgs);
            case CASH -> updateCashPrice(asset, batchArgs);
            default -> {
                log.warn("Price refresh not supported for asset type: {} ({})",
                        asset.getAssetType(), asset.getSymbol());
                yield CompletableFuture.completedFuture(null);
            }
        };
    }

    private CompletableFuture<Void> fetchStockPrice(Asset asset, List<Object[]> batchArgs) {
        return CompletableFuture.runAsync(() -> {
            try {
                BigDecimal price = stockPriceService.fetchStockPrice(asset.getSymbol());
                batchArgs.add(new Object[]{price, asset.getId()});
                log.info("Fetched price for {} ({}): {}",
                        asset.getSymbol(), asset.getAssetType(), price);
            } catch (Exception e) {
                log.error("Failed to fetch price for {} ({}) after all retries: {}",
                        asset.getSymbol(), asset.getAssetType(), e.getMessage());
                throw e;
            }
        }, priceRefreshExecutor);
    }

    private CompletableFuture<Void> updateCashPrice(Asset asset, List<Object[]> batchArgs) {
        return CompletableFuture.runAsync(() -> {
            try {
                BigDecimal rate = exchangeRateService.getRate(
                        asset.getCurrency(),
                        DEFAULT_CURRENCY);
                batchArgs.add(new Object[]{rate, asset.getId()});
                log.info("Updated rate for {} (CASH): {}", asset.getSymbol(), rate);
            } catch (Exception e) {
                log.error("Failed to fetch price for {} ({}) after all retries: {}",
                        asset.getSymbol(), asset.getAssetType(), e.getMessage());
                throw e;
            }
        }, priceRefreshExecutor);
    }
}
