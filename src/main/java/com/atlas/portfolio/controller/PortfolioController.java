package com.atlas.portfolio.controller;

import com.atlas.portfolio.dto.request.CreatePortfolioRequest;
import com.atlas.portfolio.dto.request.UpdatePortfolioRequest;
import com.atlas.portfolio.dto.response.PortfolioResponse;
import com.atlas.portfolio.service.PortfolioService;
import com.atlas.portfolio.util.SecurityUtil;
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

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        PortfolioResponse response = portfolioService.createPortfolio(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<PortfolioResponse> portfolios = portfolioService.getAllPortfolios(userId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        PortfolioResponse portfolio = portfolioService.getPortfolioById(id, userId);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        PortfolioResponse portfolio = portfolioService.updatePortfolio(id, request, userId);
        return ResponseEntity.ok(portfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        portfolioService.deletePortfolio(id, userId);
        return ResponseEntity.noContent().build();
    }
}
