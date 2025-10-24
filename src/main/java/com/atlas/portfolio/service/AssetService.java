package com.atlas.portfolio.service;

import com.atlas.portfolio.dto.request.CreateAssetRequest;
import com.atlas.portfolio.dto.request.UpdateAssetRequest;
import com.atlas.portfolio.dto.response.AssetResponse;
import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.Portfolio;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.AssetRepository;
import com.atlas.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional
    public AssetResponse createAsset(Long portfolioId, CreateAssetRequest request, Long userId) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + portfolioId));

        Asset asset = new Asset();
        asset.setPortfolio(portfolio);
        asset.setSymbol(request.getSymbol().toUpperCase());
        asset.setAssetType(request.getAssetType());
        asset.setQuantity(request.getQuantity());
        asset.setAveragePurchasePrice(request.getAveragePurchasePrice());

        Asset savedAsset = assetRepository.save(asset);
        return new AssetResponse(savedAsset);
    }

    public List<AssetResponse> getAllAssets(Long portfolioId, Long userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        return assetRepository.findByPortfolioId(portfolioId).stream()
                .map(AssetResponse::new)
                .collect(Collectors.toList());
    }

    public AssetResponse getAssetById(Long portfolioId, Long assetId, Long userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        Asset asset = assetRepository.findByIdAndPortfolioId(assetId, portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));
        return new AssetResponse(asset);
    }

    @Transactional
    public AssetResponse updateAsset(Long portfolioId, Long assetId, UpdateAssetRequest request, Long userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        Asset asset = assetRepository.findByIdAndPortfolioId(assetId, portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));

        if (request.getQuantity() != null) {
            asset.setQuantity(request.getQuantity());
        }
        if (request.getAveragePurchasePrice() != null) {
            asset.setAveragePurchasePrice(request.getAveragePurchasePrice());
        }

        Asset updatedAsset = assetRepository.save(asset);
        return new AssetResponse(updatedAsset);
    }

    @Transactional
    public void deleteAsset(Long portfolioId, Long assetId, Long userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new ResourceNotFoundException("Portfolio not found with id: " + portfolioId);
        }

        Asset asset = assetRepository.findByIdAndPortfolioId(assetId, portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));
        assetRepository.delete(asset);
    }
}
