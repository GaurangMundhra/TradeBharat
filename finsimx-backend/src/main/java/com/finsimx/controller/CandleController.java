package com.finsimx.controller;

import com.finsimx.dto.ApiResponse;
import com.finsimx.dto.ws.CandleDTO;
import com.finsimx.service.CandleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for fetching historical candlestick (OHLC) data
 * 
 * Supports both live WebSocket streaming and REST-based historical data
 * retrieval
 * Enables frontend charting with both real-time and historical candles
 */
@RestController
@RequestMapping("/candles")
@RequiredArgsConstructor
@Slf4j
public class CandleController {

    private final CandleService candleService;

    /**
     * GET /api/candles
     * 
     * Fetch historical candlestick data for charting
     * 
     * Query Parameters:
     * - asset (REQUIRED): Trading symbol (e.g., "AAPL")
     * - interval (OPTIONAL): Candle interval, default = "1m"
     * Supported: 1m, 5m, 15m, 1h, 4h, 1d
     * - limit (OPTIONAL): Max number of candles to return, default = 100
     * 
     * Example Requests:
     * - GET /api/candles?asset=AAPL
     * - GET /api/candles?asset=GOOGL&interval=5m&limit=50
     * - GET /api/candles?asset=MSFT&interval=1h&limit=200
     * 
     * @param asset    Trading asset symbol (required)
     * @param interval Candle interval (optional, default: "1m")
     * @param limit    Maximum candles to return (optional, default: 100)
     * @return List of CandleDTO objects ordered by time (newest first)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getCandles(
            @RequestParam String asset,
            @RequestParam(defaultValue = "1m") String interval,
            @RequestParam(defaultValue = "100") int limit) {

        // Validation: asset is required
        if (asset == null || asset.trim().isEmpty()) {
            log.warn("GET /candles - Missing required parameter: asset");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Asset parameter is required", 400));
        }

        // Normalize asset to uppercase
        asset = asset.trim().toUpperCase();

        // Validation: limit must be positive
        if (limit <= 0) {
            log.warn("GET /candles - Invalid limit: {}", limit);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Limit must be greater than 0", 400));
        }

        // Validation: limit reasonable cap to prevent memory abuse
        if (limit > 1000) {
            log.warn("GET /candles - Limit too high: {}, capping to 1000", limit);
            limit = 1000;
        }

        log.info("Fetching {} candles for asset {} interval {}", limit, asset, interval);

        // Fetch from CandleService
        List<CandleDTO> candles = candleService.getCompletedCandles(asset, interval, limit);

        log.debug("Retrieved {} candles for {}", candles.size(), asset);

        // Return response
        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Retrieved %d candles for %s (%s)", candles.size(), asset, interval),
                        candles));
    }

    /**
     * GET /api/candles/{asset}
     * 
     * Convenience endpoint for fetching candles by asset path parameter
     * Defaults to 1-minute interval and 100 candles
     * 
     * Example:
     * - GET /api/candles/AAPL
     * 
     * @param asset Trading asset symbol (from path)
     * @return List of CandleDTO objects
     */
    @GetMapping("/{asset}")
    public ResponseEntity<ApiResponse<?>> getCandlesByAsset(
            @PathVariable String asset) {

        // Redirect to main endpoint with defaults
        return getCandles(asset, "1m", 100);
    }

    /**
     * GET /api/candles/{asset}/{interval}
     * 
     * Convenience endpoint with asset and interval as path parameters
     * Defaults to 100 candles
     * 
     * Example:
     * - GET /api/candles/AAPL/5m
     * 
     * @param asset    Trading asset symbol (from path)
     * @param interval Candle interval (from path)
     * @return List of CandleDTO objects
     */
    @GetMapping("/{asset}/{interval}")
    public ResponseEntity<ApiResponse<?>> getCandlesByAssetAndInterval(
            @PathVariable String asset,
            @PathVariable String interval) {

        // Redirect to main endpoint with default limit
        return getCandles(asset, interval, 100);
    }

    /**
     * Health check for candle API
     * GET /api/candles/health
     * 
     * @return API status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        log.debug("Candle API health check");
        return ResponseEntity.ok(
                ApiResponse.success("Candle API is healthy", "OK"));
    }
}
