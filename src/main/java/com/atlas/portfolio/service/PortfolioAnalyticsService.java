package com.atlas.portfolio.service;

import com.atlas.portfolio.dto.response.PortfolioSummaryResponse;
import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.Portfolio;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.AssetRepository;
import com.atlas.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAnalyticsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int PERCENTAGE_SCALE = 4;

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;

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
        BigDecimal totalProfitLossPercent = calculatePercentage(totalProfitLoss, totalInvested);

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

    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            return value.divide(total, PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                    .multiply(ONE_HUNDRED);
        }
        return BigDecimal.ZERO;
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

                    BigDecimal portfolioPercentage = calculatePercentage(totalValue, totalPortfolioValue);
                    BigDecimal profitLossPercent = calculatePercentage(profitLoss, totalInvested);

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
}
