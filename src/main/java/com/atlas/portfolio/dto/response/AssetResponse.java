package com.atlas.portfolio.dto.response;

import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {

    private Long id;
    private String symbol;
    private AssetType assetType;
    private BigDecimal quantity;
    private BigDecimal averagePurchasePrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AssetResponse(Asset asset) {
        this.id = asset.getId();
        this.symbol = asset.getSymbol();
        this.assetType = asset.getAssetType();
        this.quantity = asset.getQuantity();
        this.averagePurchasePrice = asset.getAveragePurchasePrice();
        this.createdAt = asset.getCreatedAt();
        this.updatedAt = asset.getUpdatedAt();
    }
}
