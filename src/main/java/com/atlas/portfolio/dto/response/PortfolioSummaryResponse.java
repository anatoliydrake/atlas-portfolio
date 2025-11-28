package com.atlas.portfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    private Long portfolioId;
    private String portfolioName;
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private Integer totalAssets;
    private LocalDateTime lastPriceUpdate;
    private List<AssetTypeBreakdown> assetTypeBreakdowns;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetTypeBreakdown {
        private String assetType;
        private BigDecimal totalValue;
        private BigDecimal totalInvested;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
        private BigDecimal portfolioPercentage;
        private Integer count;
    }
}
