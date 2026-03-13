package com.finsimx.service;

import com.finsimx.entity.Order;
import com.finsimx.entity.Trade;
import com.finsimx.entity.User;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.*;

/**
 * Order Book Management
 * Maintains separate lists for BUY and SELL orders sorted by:
 * - BUY orders: Price descending (highest price first)
 * - SELL orders: Price ascending (lowest price first)
 * - Both: CreatedAt ascending (FIFO for same price level)
 */
@Getter
public class OrderBook {

    private final String asset;
    private final PriorityQueue<Order> buyOrders; // Max heap (highest price first)
    private final PriorityQueue<Order> sellOrders; // Min heap (lowest price first)

    public OrderBook(String asset) {
        this.asset = asset;

        // BUY orders: Sort by price DESC, then by createdAt ASC (FIFO at same price)
        this.buyOrders = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o2.getPrice().compareTo(o1.getPrice()); // DESC
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt()); // ASC
        });

        // SELL orders: Sort by price ASC, then by createdAt ASC (FIFO at same price)
        this.sellOrders = new PriorityQueue<>((o1, o2) -> {
            int priceCompare = o1.getPrice().compareTo(o2.getPrice()); // ASC
            if (priceCompare != 0)
                return priceCompare;
            return o1.getCreatedAt().compareTo(o2.getCreatedAt()); // ASC
        });
    }

    /**
     * Add order to order book
     */
    public void addOrder(Order order) {
        if (order.getType() == Order.OrderType.BUY) {
            buyOrders.offer(order);
        } else {
            sellOrders.offer(order);
        }
    }

    /**
     * Get matchable BUY orders (against a SELL order)
     * Returns BUY orders where price >= sellPrice, sorted by priority
     */
    public List<Order> getMatchableBuyOrders(BigDecimal sellPrice) {
        List<Order> matchable = new ArrayList<>();
        for (Order buyOrder : buyOrders) {
            if (buyOrder.getPrice().compareTo(sellPrice) >= 0) {
                matchable.add(buyOrder);
            } else {
                break; // Since queue is sorted, no more matches
            }
        }
        return matchable;
    }

    /**
     * Get matchable SELL orders (against a BUY order)
     * Returns SELL orders where price <= buyPrice, sorted by priority
     */
    public List<Order> getMatchableSellOrders(BigDecimal buyPrice) {
        List<Order> matchable = new ArrayList<>();
        for (Order sellOrder : sellOrders) {
            if (sellOrder.getPrice().compareTo(buyPrice) <= 0) {
                matchable.add(sellOrder);
            } else {
                break; // Since queue is sorted, no more matches
            }
        }
        return matchable;
    }

    /**
     * Remove order from order book
     */
    public boolean removeOrder(Order order) {
        if (order.getType() == Order.OrderType.BUY) {
            return buyOrders.remove(order);
        } else {
            return sellOrders.remove(order);
        }
    }

    /**
     * Check if order exists in book
     */
    public boolean contains(Order order) {
        if (order.getType() == Order.OrderType.BUY) {
            return buyOrders.contains(order);
        } else {
            return sellOrders.contains(order);
        }
    }

    /**
     * Get depth at price level
     */
    public BigDecimal getBidDepth(BigDecimal price) {
        return buyOrders.stream()
                .filter(o -> o.getPrice().equals(price))
                .map(o -> o.getQuantity().subtract(o.getFilledQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getAskDepth(BigDecimal price) {
        return sellOrders.stream()
                .filter(o -> o.getPrice().equals(price))
                .map(o -> o.getQuantity().subtract(o.getFilledQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get best bid/ask prices
     */
    public BigDecimal getBestBidPrice() {
        Order topBuy = buyOrders.peek();
        return topBuy != null ? topBuy.getPrice() : null;
    }

    public BigDecimal getBestAskPrice() {
        Order topSell = sellOrders.peek();
        return topSell != null ? topSell.getPrice() : null;
    }

    /**
     * Get order book snapshot
     */
    public Map<String, Object> getSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("asset", asset);
        snapshot.put("bestBid", getBestBidPrice());
        snapshot.put("bestAsk", getBestAskPrice());
        snapshot.put("buyDepth", buyOrders.size());
        snapshot.put("sellDepth", sellOrders.size());
        snapshot.put("spread", calculateSpread());
        return snapshot;
    }

    private BigDecimal calculateSpread() {
        BigDecimal bid = getBestBidPrice();
        BigDecimal ask = getBestAskPrice();
        if (bid != null && ask != null) {
            return ask.subtract(bid);
        }
        return null;
    }
}
