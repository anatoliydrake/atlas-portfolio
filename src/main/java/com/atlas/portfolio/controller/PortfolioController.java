package com.atlas.portfolio.controller;

import com.atlas.portfolio.dto.request.CreatePortfolioRequest;
import com.atlas.portfolio.dto.request.UpdatePortfolioRequest;
import com.atlas.portfolio.dto.response.PortfolioResponse;
import com.atlas.portfolio.dto.response.PortfolioSummaryResponse;
import com.atlas.portfolio.service.PortfolioAnalyticsService;
import com.atlas.portfolio.service.PortfolioService;
import com.atlas.portfolio.service.PriceRefreshService;
import com.atlas.portfolio.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioAnalyticsService portfolioAnalyticsService;
    private final PriceRefreshService priceRefreshService;
    private final SecurityService securityService;

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        Long userId = securityService.getCurrentUserId();
        PortfolioResponse response = portfolioService.createPortfolio(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios() {
        Long userId = securityService.getCurrentUserId();
        List<PortfolioResponse> portfolios = portfolioService.getAllPortfolios(userId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        PortfolioResponse portfolio = portfolioService.getPortfolioById(id, userId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        PortfolioSummaryResponse summary = portfolioAnalyticsService.getPortfolioSummary(id, userId);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Long userId = securityService.getCurrentUserId();
        PortfolioResponse portfolio = portfolioService.updatePortfolio(id, request, userId);
        return ResponseEntity.ok(portfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        portfolioService.deletePortfolio(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/refresh-prices")
    public ResponseEntity<Void> refreshPortfolioPrices(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        priceRefreshService.refreshPortfolioPrices(id, userId);
        return ResponseEntity.ok().build();
    }
}
