package com.atlas.portfolio.dto.response;

import com.atlas.portfolio.entity.Portfolio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private Long id;
    private String name;
    private String description;
    private Long userId;
    private List<AssetResponse> assets;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PortfolioResponse(Portfolio portfolio) {
        this.id = portfolio.getId();
        this.name = portfolio.getName();
        this.description = portfolio.getDescription();
        this.userId = portfolio.getUserId();
        this.assets = portfolio.getAssets().stream()
                .map(AssetResponse::new)
                .collect(Collectors.toList());
        this.createdAt = portfolio.getCreatedAt();
        this.updatedAt = portfolio.getUpdatedAt();
    }
}
