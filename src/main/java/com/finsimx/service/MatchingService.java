package com.finsimx.service;

import com.finsimx.dto.TradeResponse;
import com.finsimx.entity.Order;
import com.finsimx.entity.Trade;
import com.finsimx.exception.OrderException;
import com.finsimx.repository.OrderRepository;
import com.finsimx.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Order Matching Engine
 * Implements price-time priority matching algorithm:
 * - BUY orders sorted by price DESC (highest price first)
 * - SELL orders sorted by price ASC (lowest price first)
 * - Same price: FIFO (first in, first out)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletService walletService;

    // Order books per asset: asset -> OrderBook
    private final Map<String, OrderBook> orderBooks = Collections.synchronizedMap(new HashMap<>());

    /**
     * Match a new order against existing orders
     * Returns list of generated trades
     */
    @Transactional
    public List<TradeResponse> matchOrder(Order newOrder) {
        List<Trade> generatedTrades = new ArrayList<>();

        try {
            OrderBook book = orderBooks.computeIfAbsent(
                    newOrder.getAsset(),
                    asset -> new OrderBook(asset));

            if (newOrder.getType() == Order.OrderType.BUY) {
                generatedTrades = matchBuyOrder(newOrder, book);
            } else {
                generatedTrades = matchSellOrder(newOrder, book);
            }

            // Add remaining order to book if not fully filled
            if (newOrder.getFilledQuantity().compareTo(newOrder.getQuantity()) < 0) {
                book.addOrder(newOrder);
                log.info("Order {} added to {} book with {} remaining",
                        newOrder.getId(), newOrder.getAsset(),
                        newOrder.getQuantity().subtract(newOrder.getFilledQuantity()));
            }

            log.info("Order {} matched with {} trades", newOrder.getId(), generatedTrades.size());

        } catch (Exception e) {
            log.error("Error matching order {}: {}", newOrder.getId(), e.getMessage());
            throw new OrderException("MATCHING_ERROR", 500, "Error matching order: " + e.getMessage());
        }

        return generatedTrades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Match BUY order against existing SELL orders
     */
    private List<Trade> matchBuyOrder(Order buyOrder, OrderBook book) {
        List<Trade> trades = new ArrayList<>();
        BigDecimal remainingQuantity = buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity());

        List<Order> matchableSellOrders = book.getMatchableSellOrders(buyOrder.getPrice());

        for (Order sellOrder : matchableSellOrders) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Don't match with own order
            if (buyOrder.getUser().getId().equals(sellOrder.getUser().getId())) {
                continue;
            }

            BigDecimal sellRemaining = sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity());
            BigDecimal tradeQuantity = remainingQuantity.min(sellRemaining);

            if (tradeQuantity.compareTo(BigDecimal.ZERO) > 0) {
                Trade trade = executeTrade(buyOrder, sellOrder, tradeQuantity, sellOrder.getPrice());
                trades.add(trade);

                // Update remaining quantities
                remainingQuantity = remainingQuantity.subtract(tradeQuantity);

                // If SELL order fully filled, remove from book
                if (sellOrder.getFilledQuantity().compareTo(sellOrder.getQuantity()) >= 0) {
                    book.removeOrder(sellOrder);
                    sellOrder.setStatus(Order.OrderStatus.FILLED);
                    orderRepository.save(sellOrder);
                }
            }
        }

        // Update BUY order status
        updateOrderStatus(buyOrder);
        orderRepository.save(buyOrder);

        return trades;
    }

    /**
     * Match SELL order against existing BUY orders
     */
    private List<Trade> matchSellOrder(Order sellOrder, OrderBook book) {
        List<Trade> trades = new ArrayList<>();
        BigDecimal remainingQuantity = sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity());

        List<Order> matchableBuyOrders = book.getMatchableBuyOrders(sellOrder.getPrice());

        for (Order buyOrder : matchableBuyOrders) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            // Don't match with own order
            if (sellOrder.getUser().getId().equals(buyOrder.getUser().getId())) {
                continue;
            }

            BigDecimal buyRemaining = buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity());
            BigDecimal tradeQuantity = remainingQuantity.min(buyRemaining);

            if (tradeQuantity.compareTo(BigDecimal.ZERO) > 0) {
                Trade trade = executeTrade(buyOrder, sellOrder, tradeQuantity, buyOrder.getPrice());
                trades.add(trade);

                // Update remaining quantities
                remainingQuantity = remainingQuantity.subtract(tradeQuantity);

                // If BUY order fully filled, remove from book
                if (buyOrder.getFilledQuantity().compareTo(buyOrder.getQuantity()) >= 0) {
                    book.removeOrder(buyOrder);
                    buyOrder.setStatus(Order.OrderStatus.FILLED);
                    orderRepository.save(buyOrder);
                }
            }
        }

        // Update SELL order status
        updateOrderStatus(sellOrder);
        orderRepository.save(sellOrder);

        return trades;
    }

    /**
     * Execute a trade between BUY and SELL orders
     * Trade price: the price of the first order (maker's price)
     */
    @Transactional
    private Trade executeTrade(Order buyOrder, Order sellOrder, BigDecimal quantity, BigDecimal price) {
        // Create trade record
        Trade trade = Trade.builder()
                .buyer(buyOrder.getUser())
                .seller(sellOrder.getUser())
                .asset(buyOrder.getAsset())
                .price(price)
                .quantity(quantity)
                .build();

        // Update filled quantities
        buyOrder.setFilledQuantity(buyOrder.getFilledQuantity().add(quantity));
        sellOrder.setFilledQuantity(sellOrder.getFilledQuantity().add(quantity));

        // Save trade
        trade = tradeRepository.save(trade);

        log.info("Trade executed: {} {} @ {} = {} (Buy: {}, Sell: {})",
                quantity, trade.getAsset(), price, trade.getPrice().multiply(trade.getQuantity()),
                buyOrder.getUser().getUsername(), sellOrder.getUser().getUsername());

        return trade;
    }

    /**
     * Update order status based on filled quantity
     */
    private void updateOrderStatus(Order order) {
        BigDecimal filled = order.getFilledQuantity();
        BigDecimal total = order.getQuantity();

        if (filled.compareTo(total) >= 0) {
            order.setStatus(Order.OrderStatus.FILLED);
        } else if (filled.compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(Order.OrderStatus.PARTIAL);
        }
        // else: remain OPEN
    }

    /**
     * Process cancelled order - remove from order book
     */
    @Transactional
    public void processCancelledOrder(Order order) {
        OrderBook book = orderBooks.get(order.getAsset());
        if (book != null) {
            book.removeOrder(order);
            log.info("Order {} removed from {} order book", order.getId(), order.getAsset());
        }
    }

    /**
     * Get order book snapshot for an asset
     */
    public Map<String, Object> getOrderBook(String asset) {
        OrderBook book = orderBooks.get(asset);
        if (book == null) {
            return Collections.singletonMap("asset", asset);
        }
        return book.getSnapshot();
    }

    /**
     * Get trades for user (as buyer or seller)
     */
    @Transactional(readOnly = true)
    public List<TradeResponse> getUserTrades(Long userId) {
        List<Trade> trades = tradeRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId);
        return trades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get recent trades for an asset
     */
    @Transactional(readOnly = true)
    public List<TradeResponse> getAssetTrades(String asset, int limit) {
        List<Trade> trades = tradeRepository.findRecentTradesByAsset(asset, limit);
        return trades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get market price for an asset (last trade price)
     */
    @Transactional(readOnly = true)
    public BigDecimal getMarketPrice(String asset) {
        List<Trade> trades = tradeRepository.findRecentTradesByAsset(asset, 1);
        if (trades.isEmpty()) {
            return null;
        }
        return trades.get(0).getPrice();
    }

    /**
     * Get market stats for an asset
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMarketStats(String asset) {
        List<Trade> trades = tradeRepository.findByAssetOrderByCreatedAtDesc(asset);

        Map<String, Object> stats = new HashMap<>();
        stats.put("asset", asset);
        stats.put("lastPrice", trades.isEmpty() ? null : trades.get(0).getPrice());
        stats.put("openPrice", trades.isEmpty() ? null : trades.get(trades.size() - 1).getPrice());
        stats.put("tradeCount", trades.size());

        if (!trades.isEmpty()) {
            BigDecimal highPrice = trades.stream()
                    .map(Trade::getPrice)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            BigDecimal lowPrice = trades.stream()
                    .map(Trade::getPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            BigDecimal totalVolume = trades.stream()
                    .map(Trade::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.put("highPrice", highPrice);
            stats.put("lowPrice", lowPrice);
            stats.put("totalVolume", totalVolume);
        }

        // Add order book snapshot
        stats.put("orderBook", getOrderBook(asset));

        return stats;
    }
}
