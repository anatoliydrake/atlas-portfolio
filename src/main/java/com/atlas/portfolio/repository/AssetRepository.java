package com.atlas.portfolio.repository;

import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByPortfolioId(Long portfolioId);

    Optional<Asset> findByIdAndPortfolioId(Long id, Long portfolioId);

    List<Asset> findByPortfolioIdAndAssetType(Long portfolioId, AssetType assetType);

    Optional<Asset> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    boolean existsByIdAndPortfolioId(Long id, Long portfolioId);
}
