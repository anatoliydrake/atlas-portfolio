package com.atlas.portfolio.controller;

import com.atlas.portfolio.dto.request.CreateAssetRequest;
import com.atlas.portfolio.dto.request.UpdateAssetRequest;
import com.atlas.portfolio.dto.response.AssetResponse;
import com.atlas.portfolio.service.AssetService;
import com.atlas.portfolio.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreateAssetRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AssetResponse response = assetService.createAsset(portfolioId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAllAssets(@PathVariable Long portfolioId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<AssetResponse> assets = assetService.getAllAssets(portfolioId, userId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<AssetResponse> getAssetById(
            @PathVariable Long portfolioId,
            @PathVariable Long assetId) {
        Long userId = SecurityUtil.getCurrentUserId();
        AssetResponse asset = assetService.getAssetById(portfolioId, assetId, userId);
        return ResponseEntity.ok(asset);
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable Long portfolioId,
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AssetResponse asset = assetService.updateAsset(portfolioId, assetId, request, userId);
        return ResponseEntity.ok(asset);
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long portfolioId,
            @PathVariable Long assetId) {
        Long userId = SecurityUtil.getCurrentUserId();
        assetService.deleteAsset(portfolioId, assetId, userId);
        return ResponseEntity.noContent().build();
    }
}
