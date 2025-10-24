package com.atlas.portfolio.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal averagePurchasePrice;
}
