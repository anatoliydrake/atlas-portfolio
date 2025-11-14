package com.atlas.portfolio.service.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinnhubQuoteResponse {

    @JsonProperty("c")
    private BigDecimal currentPrice;
}
