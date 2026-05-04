package com.finsimx.service;

import com.finsimx.dto.ws.CandleDTO;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Candle (OHLC) Engine
 * Generates real-time candlestick data based on executed trades
 * 
 * Thread-safe using ConcurrentHashMap
 * Supports multiple intervals (currently focused on 1-minute)
 * Automatically rotates candles when interval window expires
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CandleService {

    public static final String INTERVAL_1M = "1m";
    private static final long INTERVAL_1M_MS = 60 * 1000; // 60 seconds in milliseconds

    private final NotificationService notificationService;
    /**
     * Candle storage per asset + interval
     * Key format: "ASSET:INTERVAL" (e.g., "AAPL:1m")
     * Value: Current active candle for that asset+interval combo
     */
    private final Map<String, CandleDTO> activeCandles = new ConcurrentHashMap<>();

    /**
     * Store completed candles for historical data
     * Key format: "ASSET:INTERVAL"
     * Value: Deque of completed candles (newest first)
     */
    private final Map<String, Deque<CandleDTO>> completedCandles = new ConcurrentHashMap<>();

    /**
     * Update or create candle for an asset based on trade execution
     * 
     * @param asset    Trading asset (e.g., "AAPL")
     * @param price    Trade price
     * @param quantity Trade quantity
     */
    public CandleDTO updateCandle(String asset, Double price, Double quantity) {

        return updateCandle(asset, price, quantity, INTERVAL_1M);
    }

    /**
     * Update or create candle for a specific interval
     * 
     * @param asset    Trading asset
     * @param price    Trade execution price
     * @param quantity Trade execution quantity
     * @param interval Time interval (e.g., "1m")
     * @return Updated or newly created candle
     */
    public CandleDTO updateCandle(String asset, Double price, Double quantity, String interval) {

        String candleKey = generateCandleKey(asset, interval);
        long intervalMS = getIntervalInMillis(interval);
        long currentTime = System.currentTimeMillis();
        long currentWindowStart = getWindowStart(currentTime, intervalMS);

        CandleDTO candle = activeCandles.get(candleKey);

        // 🔥 HANDLE ROTATION FIRST
        if (candle != null && candle.getEndTime() <= currentTime) {
            finalizeCandle(candleKey, candle);
            candle = null;
        }

        // 🔥 CREATE NEW CANDLE IF NULL
        if (candle == null) {
            candle = new CandleDTO();
            candle.setAsset(asset);
            candle.setInterval(interval);
            candle.setStartTime(currentWindowStart);
            candle.setEndTime(currentWindowStart + intervalMS);
            candle.setOpen(price);
            candle.setHigh(price + 1);
            candle.setLow(price - 1);
            candle.setClose(price);
            candle.setVolume(quantity);

            log.debug("New candle created: {} {}", asset, interval);

        } else {
            // 🔥 UPDATE EXISTING
            candle.setHigh(Math.max(candle.getHigh(), price));
            candle.setLow(Math.min(candle.getLow(), price));
            candle.setClose(price);
            candle.setVolume(candle.getVolume() + quantity);
        }

        // 🔥 SAVE
        activeCandles.put(candleKey, candle);

        // 🔥 SAFE BROADCAST (AFTER CREATION)
        try {
            if (candle != null) {
                notificationService.notifyCandleUpdate(candle);
            }
        } catch (Exception e) {
            log.error("Candle broadcast failed", e);
        }

        return candle;
    }

    /**
     * Get the current active candle for an asset
     * 
     * @param asset Trading asset
     * @return Current candle or null if not exists
     */
    public CandleDTO getCurrentCandle(String asset) {
        return getCurrentCandle(asset, INTERVAL_1M);
    }

    /**
     * Get the current active candle for a specific interval
     */
    public CandleDTO getCurrentCandle(String asset, String interval) {
        String candleKey = generateCandleKey(asset, interval);
        return activeCandles.get(candleKey);
    }

    /**
     * Get completed candles (historical data)
     * 
     * @param asset Trading asset
     * @param limit Number of candles to retrieve
     * @return List of completed candles (newest first)
     */
    public List<CandleDTO> getCompletedCandles(String asset, int limit) {
        return getCompletedCandles(asset, INTERVAL_1M, limit);
    }

    /**
     * Get completed candles for a specific interval
     */
    public List<CandleDTO> getCompletedCandles(String asset, String interval, int limit) {
        String candleKey = generateCandleKey(asset, interval);
        Deque<CandleDTO> candles = completedCandles.get(candleKey);

        if (candles == null || candles.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(candles).stream()
                .limit(limit)
                .toList();
    }

    /**
     * Finalize a candle (close current interval, store in history)
     * Called when candle interval window expires
     */
    private void finalizeCandle(String candleKey, CandleDTO candle) {
        completedCandles.computeIfAbsent(candleKey, k -> new LinkedList<>())
                .addFirst(candle); // Add to front (newest first)

        log.info("Candle finalized: {} {} (V:{}) stored in history",
                candle.getAsset(), candle.getInterval(), candle.getVolume());
    }

    /**
     * Generate candle storage key from asset and interval
     */
    private String generateCandleKey(String asset, String interval) {
        return asset.toUpperCase() + ":" + interval;
    }

    /**
     * Get interval duration in milliseconds
     */
    private long getIntervalInMillis(String interval) {
        return switch (interval) {
            case "1m" -> 60 * 1000;
            case "5m" -> 5 * 60 * 1000;
            case "15m" -> 15 * 60 * 1000;
            case "1h" -> 60 * 60 * 1000;
            case "4h" -> 4 * 60 * 60 * 1000;
            case "1d" -> 24 * 60 * 60 * 1000;
            default -> 60 * 1000; // Default to 1 minute
        };
    }

    /**
     * Calculate the start time of the current window for a given interval
     * E.g., for 1m interval and current time 14:23:45, returns 14:23:00
     */
    private long getWindowStart(long timestamp, long intervalMS) {
        return (timestamp / intervalMS) * intervalMS;
    }

    /**
     * Clear all candles (for testing/reset purposes)
     */
    public void clearAll() {
        activeCandles.clear();
        completedCandles.clear();
        log.info("All candles cleared");
    }

    /**
     * Get all active candles (single snapshot)
     * Useful for monitoring/debugging
     */
    public Map<String, CandleDTO> getAllActiveCandles() {
        return new ConcurrentHashMap<>(activeCandles);
    }

    @Scheduled(fixedRate = 5000) // every 5 seconds
    public void rotateCandles() {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, CandleDTO> entry : activeCandles.entrySet()) {
            CandleDTO candle = entry.getValue();

            if (candle.getEndTime() <= currentTime) {
                finalizeCandle(entry.getKey(), candle);
                // 🔥 BROADCAST FINAL CANDLE
                if (candle != null) {
                    try {
                        notificationService.notifyCandleUpdate(candle);
                    } catch (Exception e) {
                        log.error("Rotate broadcast failed", e);
                    }
                }
                activeCandles.remove(entry.getKey());

                log.info("Auto-rotated candle: {}", entry.getKey());
            }
        }
    }
}
