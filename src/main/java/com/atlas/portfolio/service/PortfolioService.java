package com.atlas.portfolio.service;

import com.atlas.portfolio.dto.request.CreatePortfolioRequest;
import com.atlas.portfolio.dto.request.UpdatePortfolioRequest;
import com.atlas.portfolio.dto.response.PortfolioResponse;
import com.atlas.portfolio.dto.response.PortfolioSummaryResponse;
import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.Portfolio;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.AssetRepository;
import com.atlas.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final StockPriceService stockPriceService;
    private final ExchangeRateService exchangeRateService;
    private final JdbcTemplate jdbcTemplate;
    private final Executor priceRefreshExecutor;

    @Transactional
    public PortfolioResponse createPortfolio(CreatePortfolioRequest request, Long userId) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setUserId(userId);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return new PortfolioResponse(savedPortfolio);
    }

    public List<PortfolioResponse> getAllPortfolios(Long userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(PortfolioResponse::new)
                .collect(Collectors.toList());
    }

    public PortfolioResponse getPortfolioById(Long id, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
        return new PortfolioResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(Long id, UpdatePortfolioRequest request, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));

        if (request.getName() != null) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }

        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        return new PortfolioResponse(updatedPortfolio);
    }

    @Transactional
    public void deletePortfolio(Long id, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
        portfolioRepository.delete(portfolio);
    }

    public PortfolioSummaryResponse getPortfolioSummary(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));
        List<Asset> assets = assetRepository.findByPortfolioId(portfolioId);

        if (assets.isEmpty()) {
            return new PortfolioSummaryResponse(
                    portfolio.getId(),
                    portfolio.getName(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    null,
                    List.of()
            );
        }

        BigDecimal totalValue = getTotalValue(assets);
        BigDecimal totalInvested = getTotalInvested(assets);
        LocalDateTime lastPriceUpdate = assets.stream()
                .map(Asset::getPriceUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        BigDecimal totalProfitLoss = totalValue.subtract(totalInvested);
        BigDecimal totalProfitLossPercent = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalProfitLossPercent = totalProfitLoss
                    .divide(totalInvested, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return new PortfolioSummaryResponse(
                portfolio.getId(),
                portfolio.getName(),
                totalValue,
                totalInvested,
                totalProfitLoss,
                totalProfitLossPercent,
                assets.size(),
                lastPriceUpdate,
                getBreakdown(assets, totalValue)
        );
    }

    private BigDecimal getTotalValue(List<Asset> assets) {
        return assets.stream()
                .filter(asset -> asset.getCurrentPrice() != null)
                .map(asset -> asset.getCurrentPrice().multiply(asset.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalInvested(List<Asset> assets) {
        return assets.stream()
                .map(asset -> asset.getAveragePurchasePrice().multiply(asset.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<PortfolioSummaryResponse.AssetTypeBreakdown> getBreakdown(
            List<Asset> assets, BigDecimal totalPortfolioValue) {
        Map<String, List<Asset>> assetsByType = assets.stream()
                .collect(Collectors.groupingBy(asset -> asset.getAssetType().name()));
        return assetsByType.entrySet().stream()
                .map(entry -> {
                    String assetType = entry.getKey();
                    List<Asset> typeAssets = entry.getValue();

                    BigDecimal totalValue = getTotalValue(typeAssets);
                    BigDecimal totalInvested = getTotalInvested(typeAssets);
                    BigDecimal profitLoss = totalValue.subtract(totalInvested);

                    BigDecimal portfolioPercentage = BigDecimal.ZERO;
                    if (totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
                        portfolioPercentage = totalValue
                                .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                    }

                    BigDecimal profitLossPercent = BigDecimal.ZERO;
                    if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                        profitLossPercent = profitLoss
                                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                    }

                    return new PortfolioSummaryResponse.AssetTypeBreakdown(
                            assetType,
                            totalValue,
                            totalInvested,
                            profitLoss,
                            profitLossPercent,
                            portfolioPercentage,
                            typeAssets.size()
                    );
                })
                .sorted((a, b) -> b.getTotalValue().compareTo(a.getTotalValue()))
                .toList();
    }

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
            case STOCK -> fetchPrice(asset, batchArgs);
            case CASH -> updateCashPrice(asset, batchArgs);
            default -> {
                log.warn("Price refresh not supported for asset type: {} ({})",
                        asset.getAssetType(), asset.getSymbol());
                yield CompletableFuture.completedFuture(null);
            }
        };
    }

    private CompletableFuture<Void> fetchPrice(Asset asset, List<Object[]> batchArgs) {
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
                        "USD");
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
