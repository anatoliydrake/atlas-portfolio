package com.atlas.portfolio.dto.request;

import com.atlas.portfolio.entity.enums.AssetType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssetRequest {

    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    private String symbol;

    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Average purchase price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal averagePurchasePrice;
}
