package com.finsimx.controller;

import com.finsimx.dto.TradeResponse;
import com.finsimx.dto.ApiResponse;
import com.finsimx.entity.User;
import com.finsimx.repository.UserRepository;
import com.finsimx.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
@Slf4j
public class TradeController {

    private final MatchingService matchingService;
    private final UserRepository userRepository;

    /**
     * GET /api/trades/me
     * Get authenticated user's trades
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getMyTrades(Authentication authentication) {

        String username = (String) authentication.getPrincipal();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TradeResponse> trades = matchingService.getUserTrades(user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("User trades retrieved successfully", trades));
    }

    /**
     * GET /api/trades/asset/{asset}
     * Get recent trades for an asset
     */
    @GetMapping("/asset/{asset}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getAssetTrades(
            @PathVariable String asset,
            @RequestParam(defaultValue = "100") int limit) {

        List<TradeResponse> trades = matchingService.getAssetTrades(asset, limit);

        return ResponseEntity.ok(
                ApiResponse.success("Asset trades retrieved successfully", trades));
    }

    /**
     * GET /api/trades/market-price/{asset}
     */
    @GetMapping("/market-price/{asset}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getMarketPrice(@PathVariable String asset) {

        var marketPrice = matchingService.getMarketPrice(asset);

        Map<String, Object> response = Map.of(
                "asset", asset,
                "price", marketPrice != null ? marketPrice : "No trades yet");

        return ResponseEntity.ok(
                ApiResponse.success("Market price retrieved successfully", response));
    }

    /**
     * GET /api/trades/order-book/{asset}
     */
    @GetMapping("/order-book/{asset}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrderBook(@PathVariable String asset) {

        var orderBook = matchingService.getOrderBook(asset);

        return ResponseEntity.ok(
                ApiResponse.success("Order book retrieved successfully", orderBook));
    }

    /**
     * GET /api/trades/market-stats/{asset}
     */
    @GetMapping("/market-stats/{asset}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getMarketStats(@PathVariable String asset) {

        var stats = matchingService.getMarketStats(asset);

        return ResponseEntity.ok(
                ApiResponse.success("Market statistics retrieved successfully", stats));
    }
}