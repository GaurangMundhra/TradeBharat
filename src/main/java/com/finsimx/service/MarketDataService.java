package com.finsimx.service;

import com.finsimx.dto.ws.*;
import com.finsimx.entity.*;
import com.finsimx.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    // In-memory price cache (in production, fetch from external API)
    private final Map<String, PriceUpdate> priceCache = new ConcurrentHashMap<>();

    /**
     * Get price update for an asset
     */
    public PriceUpdate getPriceUpdate(String asset) {
        return priceCache.getOrDefault(asset, generateMockPrice(asset));
    }

    /**
     * Get all position updates for a user
     */
    public List<PositionUpdate> getPositionUpdates(Long userId) {
        List<PositionUpdate> updates = new ArrayList<>();

        // Group orders by asset to calculate positions
        List<Order> orders = orderRepository.findByUserId(userId);
        Map<String, Long> positions = new HashMap<>();
        Map<String, Long> longPositions = new HashMap<>();
        Map<String, Long> shortPositions = new HashMap<>();

        for (Order order : orders) {
            String asset = order.getAsset();
            long quantity = order.getQuantity().longValue();

            if (order.getType() == Order.OrderType.BUY) {
                longPositions.merge(asset, quantity, Long::sum);
                positions.merge(asset, quantity, Long::sum);
            } else if (order.getType() == Order.OrderType.SELL) {
                shortPositions.merge(asset, quantity, Long::sum);
                positions.merge(asset, -quantity, Long::sum);
            }
        }

        // Create position updates
        for (Map.Entry<String, Long> entry : positions.entrySet()) {
            String asset = entry.getKey();
            Long quantity = entry.getValue();

            if (quantity != 0) {
                PriceUpdate priceUpdate = getPriceUpdate(asset);
                PositionUpdate positionUpdate = new PositionUpdate();
                positionUpdate.setUserId(userId);
                positionUpdate.setAsset(asset);
                positionUpdate.setQuantity(Math.abs(quantity));
                positionUpdate.setLongQuantity(longPositions.getOrDefault(asset, 0L));
                positionUpdate.setShortQuantity(shortPositions.getOrDefault(asset, 0L));
                positionUpdate.setCurrentPrice(priceUpdate.getCurrentPrice());
                positionUpdate.setValue(Math.abs(quantity) * priceUpdate.getCurrentPrice());
                positionUpdate.setTimestamp(System.currentTimeMillis());

                updates.add(positionUpdate);
            }
        }

        return updates;
    }

    /**
     * Get portfolio update for a user
     */
    public PortfolioUpdate getPortfolioUpdate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<PositionUpdate> positions = getPositionUpdates(userId);

        double investedValue = positions.stream()
                .mapToDouble(PositionUpdate::getValue)
                .sum();

        double unrealizedPnL = calculateUnrealizedPnL(positions);
        double totalValue = user.getBalance().doubleValue() + investedValue;

        PortfolioUpdate portfolio = new PortfolioUpdate();
        portfolio.setUserId(userId);
        portfolio.setUserName(user.getUsername());
        portfolio.setTotalValue(totalValue);
        portfolio.setCashBalance(user.getBalance().doubleValue());
        portfolio.setInvestedValue(investedValue);
        portfolio.setTotalUnrealizedPnL(unrealizedPnL);
        portfolio.setTotalUnrealizedPnLPercent(investedValue > 0 ? (unrealizedPnL / investedValue) * 100 : 0);
        portfolio.setPositionCount(positions.size());
        portfolio.setTimestamp(System.currentTimeMillis());

        return portfolio;
    }

    /**
     * Get price updates for multiple assets
     */
    public Map<String, PriceUpdate> getPriceUpdates(List<String> assets) {
        Map<String, PriceUpdate> updates = new HashMap<>();
        for (String asset : assets) {
            updates.put(asset, getPriceUpdate(asset));
        }
        return updates;
    }

    /**
     * Update price in cache
     */
    public void updatePrice(String asset, Double price) {
        PriceUpdate previous = priceCache.getOrDefault(asset, generateMockPrice(asset));
        PriceUpdate update = new PriceUpdate();
        update.setAsset(asset);
        update.setCurrentPrice(price);
        update.setPreviousPrice(previous.getCurrentPrice());
        update.setChange(price - previous.getCurrentPrice());
        update.setChangePercent((update.getChange() / previous.getCurrentPrice()) * 100);
        update.setTimestamp(System.currentTimeMillis());
        update.setVolume(System.currentTimeMillis() % 1000000);

        priceCache.put(asset, update);
    }

    /**
     * Generate mock price for demo purposes
     */
    private PriceUpdate generateMockPrice(String asset) {
        PriceUpdate update = new PriceUpdate();
        update.setAsset(asset);

        // Generate mock prices based on asset
        double basePrice = switch (asset.toUpperCase()) {
            case "AAPL" -> 150.0;
            case "GOOGL" -> 140.0;
            case "MSFT" -> 380.0;
            case "TESLA" -> 245.0;
            case "AMZN" -> 175.0;
            default -> 100.0;
        };

        // Add random variation
        double variation = (Math.random() - 0.5) * 10;
        double currentPrice = basePrice + variation;

        update.setCurrentPrice(currentPrice);
        update.setPreviousPrice(basePrice);
        update.setChange(currentPrice - basePrice);
        update.setChangePercent((update.getChange() / basePrice) * 100);
        update.setTimestamp(System.currentTimeMillis());
        update.setVolume(1000000L + (long) (Math.random() * 5000000));

        return update;
    }

    /**
     * Calculate unrealized P&L for a position
     */
    private double calculateUnrealizedPnL(List<PositionUpdate> positions) {
        double totalPnL = 0;
        for (PositionUpdate position : positions) {
            // This is a simplified calculation
            // In production, track cost basis per position
            totalPnL += (position.getCurrentPrice() - 100) * position.getQuantity();
        }
        return totalPnL;
    }

    /**
     * Simulate market data updates (runs every 5 seconds)
     */
    @Scheduled(fixedDelay = 5000)
    public void updateMarketData() {
        // Update prices for common assets
        List<String> assets = List.of("AAPL", "GOOGL", "MSFT", "TESLA", "AMZN");
        for (String asset : assets) {
            PriceUpdate current = priceCache.getOrDefault(asset, generateMockPrice(asset));
            double newPrice = current.getCurrentPrice() * (1 + (Math.random() - 0.5) * 0.001);
            updatePrice(asset, newPrice);
        }
    }

    /**
     * Get market statistics
     */
    public Map<String, Object> getMarketStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeUsers", userRepository.count());
        stats.put("totalTrades", tradeRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("marketTime", System.currentTimeMillis());
        return stats;
    }

    /**
     * Get recent trade activity
     */
    public List<Map<String, Object>> getRecentTrades(int limit) {
        List<Trade> trades = tradeRepository.findAll().stream()
                .sorted(Comparator.comparing(Trade::getCreatedAt).reversed())
                .limit(limit)
                .toList();

        return trades.stream()
                .<Map<String, Object>>map(trade -> Map.of(
                        "tradeId", trade.getId(),
                        "buyerId", trade.getBuyer().getId(),
                        "sellerId", trade.getSeller().getId(),
                        "asset", trade.getAsset(),
                        "quantity", trade.getQuantity(),
                        "price", trade.getPrice(),
                        "status", "EXECUTED",
                        // "status", trade.getStatus(),
                        "timestamp", trade.getCreatedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()))
                .toList();
    }
}
