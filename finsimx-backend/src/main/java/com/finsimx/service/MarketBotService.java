package com.finsimx.service;

import com.finsimx.entity.Order;
import com.finsimx.entity.User;
import com.finsimx.repository.OrderRepository;
import com.finsimx.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Market Bot Service
 * Creates automated market maker that places buy/sell orders
 * to provide liquidity and realistic price action.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketBotService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MatchingService matchingService;

    private static final String BOT_USERNAME = "__market_bot__";
    private static final String BOT_EMAIL = "bot@tradebharat.internal";
    private static final BigDecimal BOT_BALANCE = new BigDecimal("999999999");

    // Base prices for each asset (updated as trades happen)
    private final Map<String, Double> basePrices = new ConcurrentHashMap<>(Map.of(
            "BTC", 65000.0,
            "ETH", 3200.0,
            "AAPL", 190.0,
            "GOOGL", 175.0,
            "MSFT", 420.0,
            "TSLA", 180.0
    ));

    // Bot config
    private volatile boolean enabled = true;
    private volatile double spreadPercent = 0.5;   // 0.5% spread
    private volatile double volatility = 0.2;      // 0.2% per tick
    private volatile int maxOrdersPerAsset = 3;

    private User botUser;

    /**
     * Initialize bot user on startup
     */
    @PostConstruct
    @Transactional
    public void init() {
        Optional<User> existing = userRepository.findByUsername(BOT_USERNAME);
        if (existing.isPresent()) {
            botUser = existing.get();
            // Reset balance
            botUser.setBalance(BOT_BALANCE);
            botUser = userRepository.save(botUser);
            log.info("Market bot user found (ID: {}), balance reset", botUser.getId());
        } else {
            botUser = User.builder()
                    .username(BOT_USERNAME)
                    .email(BOT_EMAIL)
                    .password("$2a$10$botnotloginable") // Not a valid BCrypt hash
                    .role("BOT")
                    .balance(BOT_BALANCE)
                    .build();
            botUser = userRepository.save(botUser);
            log.info("Market bot user created (ID: {})", botUser.getId());
        }
    }

    /**
     * Main bot loop — runs every 5 seconds
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void runMarketMaker() {
        if (!enabled || botUser == null) return;

        try {
            for (String asset : basePrices.keySet()) {
                // Apply random walk to base price
                double drift = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * volatility / 100;
                double currentBase = basePrices.get(asset) * (1 + drift);
                basePrices.put(asset, currentBase);

                // Check existing bot orders for this asset
                long openBotOrders = orderRepository.findByUserId(botUser.getId()).stream()
                        .filter(o -> o.getAsset().equals(asset) && o.getStatus() == Order.OrderStatus.OPEN)
                        .count();

                if (openBotOrders >= maxOrdersPerAsset * 2) {
                    // Cancel oldest bot orders to make room
                    cancelOldBotOrders(asset);
                }

                // Place bid (BUY below mid)
                double bidPrice = currentBase * (1 - spreadPercent / 100);
                double qty = randomQuantity(asset);
                placeOrderSafe(asset, Order.OrderType.BUY, bidPrice, qty);

                // Place ask (SELL above mid)
                double askPrice = currentBase * (1 + spreadPercent / 100);
                qty = randomQuantity(asset);
                placeOrderSafe(asset, Order.OrderType.SELL, askPrice, qty);
            }
        } catch (Exception e) {
            log.warn("Market bot error: {}", e.getMessage());
        }
    }

    private void placeOrderSafe(String asset, Order.OrderType type, double price, double qty) {
        try {
            // Reset bot balance to prevent insufficient funds
            botUser.setBalance(BOT_BALANCE);
            userRepository.save(botUser);

            Order order = Order.builder()
                    .user(botUser)
                    .asset(asset)
                    .type(type)
                    .price(BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP))
                    .quantity(BigDecimal.valueOf(qty).setScale(2, RoundingMode.HALF_UP))
                    .filledQuantity(BigDecimal.ZERO)
                    .status(Order.OrderStatus.OPEN)
                    .build();

            order = orderRepository.save(order);
            matchingService.matchOrder(order);

            log.debug("Bot {} {} {} @ ₹{} qty {}", type, asset,
                    order.getId(), String.format("%.2f", price), String.format("%.2f", qty));
        } catch (Exception e) {
            log.debug("Bot order failed for {} {}: {}", type, asset, e.getMessage());
        }
    }

    private void cancelOldBotOrders(String asset) {
        List<Order> botOrders = orderRepository.findByUserId(botUser.getId()).stream()
                .filter(o -> o.getAsset().equals(asset) && o.getStatus() == Order.OrderStatus.OPEN)
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .toList();

        // Cancel oldest half
        int toCancel = Math.min(botOrders.size() / 2, maxOrdersPerAsset);
        for (int i = 0; i < toCancel; i++) {
            Order order = botOrders.get(i);
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            matchingService.processCancelledOrder(order);
        }
    }

    private double randomQuantity(String asset) {
        return switch (asset) {
            case "BTC" -> 0.01 + ThreadLocalRandom.current().nextDouble() * 0.5;
            case "ETH" -> 0.1 + ThreadLocalRandom.current().nextDouble() * 5;
            default -> 1 + ThreadLocalRandom.current().nextDouble() * 20;
        };
    }

    // ==================== Admin Controls ====================

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public double getSpreadPercent() { return spreadPercent; }
    public void setSpreadPercent(double spread) { this.spreadPercent = spread; }
    public double getVolatility() { return volatility; }
    public void setVolatility(double vol) { this.volatility = vol; }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("spreadPercent", spreadPercent);
        status.put("volatility", volatility);
        status.put("botUserId", botUser != null ? botUser.getId() : null);
        status.put("basePrices", new LinkedHashMap<>(basePrices));

        if (botUser != null) {
            long openOrders = orderRepository.findByUserId(botUser.getId()).stream()
                    .filter(o -> o.getStatus() == Order.OrderStatus.OPEN)
                    .count();
            status.put("openOrders", openOrders);
        }
        return status;
    }

    /**
     * Update base price for an asset (e.g. from external feed)
     */
    public void updateBasePrice(String asset, double price) {
        basePrices.put(asset, price);
    }
}
