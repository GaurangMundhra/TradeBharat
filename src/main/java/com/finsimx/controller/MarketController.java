package com.finsimx.controller;

import com.finsimx.dto.ApiResponse;
import com.finsimx.dto.ws.PriceUpdate;
import com.finsimx.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
@Slf4j
public class MarketController {

    private final MarketDataService marketDataService;

    /**
     * GET /api/market/price/{asset}
     * Get current price of an asset
     */
    @GetMapping("/price/{asset}")
    public ResponseEntity<ApiResponse<?>> getPriceUpdate(@PathVariable String asset) {
        PriceUpdate priceUpdate = marketDataService.getPriceUpdate(asset.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Price retrieved successfully", priceUpdate));
    }

    /**
     * GET /api/market/prices
     * Get current prices of multiple assets
     */
    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<?>> getPrices(@RequestParam List<String> assets) {
        Map<String, PriceUpdate> prices = marketDataService.getPriceUpdates(
                assets.stream().map(String::toUpperCase).toList());
        return ResponseEntity.ok(ApiResponse.success("Prices retrieved successfully", prices));
    }

    /**
     * GET /api/market/stats
     * Get market statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getMarketStats() {
        Map<String, Object> stats = marketDataService.getMarketStats();
        return ResponseEntity.ok(ApiResponse.success("Market stats retrieved successfully", stats));
    }

    /**
     * GET /api/market/recent-trades
     * Get recent trade activity
     */
    @GetMapping("/recent-trades")
    public ResponseEntity<ApiResponse<?>> getRecentTrades(
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> trades = marketDataService.getRecentTrades(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent trades retrieved successfully", trades));
    }

    /**
     * PUT /api/market/price/{asset}
     * Update price (admin only - for testing/simulation)
     */
    @PutMapping("/price/{asset}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> updatePrice(
            @PathVariable String asset,
            @RequestParam Double price) {
        marketDataService.updatePrice(asset.toUpperCase(), price);
        PriceUpdate updated = marketDataService.getPriceUpdate(asset.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully", updated));
    }

    /**
     * GET /api/market/supported-assets
     * Get list of supported assets
     */
    @GetMapping("/supported-assets")
    public ResponseEntity<ApiResponse<?>> getSupportedAssets() {
        List<String> assets = List.of("AAPL", "GOOGL", "MSFT", "TESLA", "AMZN");
        return ResponseEntity.ok(ApiResponse.success("Supported assets retrieved successfully", assets));
    }
}
