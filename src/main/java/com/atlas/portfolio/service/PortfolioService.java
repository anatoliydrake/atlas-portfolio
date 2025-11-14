package com.atlas.portfolio.service;

import com.atlas.portfolio.dto.request.CreatePortfolioRequest;
import com.atlas.portfolio.dto.request.UpdatePortfolioRequest;
import com.atlas.portfolio.dto.response.PortfolioResponse;
import com.atlas.portfolio.entity.Asset;
import com.atlas.portfolio.entity.Portfolio;
import com.atlas.portfolio.exception.ResourceNotFoundException;
import com.atlas.portfolio.repository.AssetRepository;
import com.atlas.portfolio.repository.PortfolioRepository;
import com.atlas.portfolio.service.external.FinnhubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final FinnhubService finnhubService;

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

    @Transactional
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

        List<CompletableFuture<Void>> futures = assets.stream()
                .map(this::refreshAssetPrice)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Finished refreshing prices for portfolio {}", portfolioId);
    }

    private CompletableFuture<Void> refreshAssetPrice(Asset asset) {
        return switch (asset.getAssetType()) {
            case STOCK, ETF -> fetchAndUpdatePrice(asset, asset.getSymbol());
            case CRYPTO -> fetchAndUpdatePrice(asset, "BINANCE:" + asset.getSymbol() + "USDT");
            case CASH -> updateCashPrice(asset);
            default -> {
                log.warn("Price refresh not supported for asset type: {} ({})",
                        asset.getAssetType(), asset.getSymbol());
                yield CompletableFuture.completedFuture(null);
            }
        };
    }

    private CompletableFuture<Void> fetchAndUpdatePrice(Asset asset, String symbol) {
        return finnhubService.fetchStockPrice(symbol)
                .thenAccept(price -> {
                    if (price != null) {
                        asset.setCurrentPrice(price);
                        asset.setPriceUpdatedAt(LocalDateTime.now());
                        assetRepository.save(asset);
                        log.info("Updated price for {} ({}): {}",
                                asset.getSymbol(), asset.getAssetType(), price);
                    } else {
                        log.warn("Failed to fetch price for {} ({})",
                                asset.getSymbol(), asset.getAssetType());
                    }
                });
    }

    private CompletableFuture<Void> updateCashPrice(Asset asset) {
        asset.setCurrentPrice(BigDecimal.ONE);
        asset.setPriceUpdatedAt(LocalDateTime.now());
        assetRepository.save(asset);
        log.info("Updated price for {} (CASH): 1.0", asset.getSymbol());
        return CompletableFuture.completedFuture(null);
    }
}
