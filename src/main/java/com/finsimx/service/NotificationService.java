package com.finsimx.service;

import com.finsimx.dto.ws.*;
import com.finsimx.websocket.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WebSocketHandler webSocketHandler;

    /**
     * Notify price update to all connected clients
     */
    public void notifyPriceUpdate(String asset, Double price) {
        PriceUpdate priceUpdate = new PriceUpdate();
        priceUpdate.setAsset(asset);
        priceUpdate.setCurrentPrice(price);
        priceUpdate.setTimestamp(System.currentTimeMillis());

        webSocketHandler.broadcastPriceUpdate(priceUpdate);
        log.debug("Price update broadcast: {} = {}", asset, price);
    }

    /**
     * Notify trade execution
     */
    public void notifyTradeExecution(Long tradeId, Long buyerId, Long sellerId,
            String buyerName, String sellerName,
            String asset, Long quantity, Double price) {
        TradeExecutionUpdate update = new TradeExecutionUpdate();
        update.setTradeId(tradeId);
        update.setBuyerId(buyerId);
        update.setSellerId(sellerId);
        update.setBuyerName(buyerName);
        update.setSellerName(sellerName);
        update.setAsset(asset);
        update.setQuantity(quantity);
        update.setPrice(price);
        update.setTotalValue(quantity * price);
        update.setStatus("SETTLED");
        update.setExecutedAt(System.currentTimeMillis());

        webSocketHandler.broadcastTradeExecution(update);
        log.info("Trade execution notification sent: Trade {}", tradeId);
    }

    /**
     * Notify position update
     */
    public void notifyPositionUpdate(Long userId, String asset, Long quantity,
            Long longQty, Long shortQty, Double currentPrice) {
        PositionUpdate update = new PositionUpdate();
        update.setUserId(userId);
        update.setAsset(asset);
        update.setQuantity(quantity);
        update.setLongQuantity(longQty);
        update.setShortQuantity(shortQty);
        update.setCurrentPrice(currentPrice);
        update.setValue(quantity * currentPrice);
        update.setTimestamp(System.currentTimeMillis());

        webSocketHandler.broadcastPositionUpdate(update);
        log.debug("Position update notification sent: User {} - Asset {}", userId, asset);
    }

    /**
     * Notify portfolio update
     */
    public void notifyPortfolioUpdate(PortfolioUpdate portfolio) {
        webSocketHandler.broadcastPortfolioUpdate(portfolio);
        log.debug("Portfolio update notification sent: User {}", portfolio.getUserId());
    }

    /**
     * Notify order matched
     */
    public void notifyOrderMatched(Long orderId, Long tradeId, String asset,
            String side, Long quantity, Double price) {
        OrderMatchedUpdate update = new OrderMatchedUpdate();
        update.setOrderId(orderId);
        update.setTradeId(tradeId);
        update.setAsset(asset);
        update.setSide(side);
        update.setQuantity(quantity);
        update.setPrice(price);
        update.setTotalValue(quantity * price);
        update.setStatus("MATCHED");
        update.setTimestamp(System.currentTimeMillis());

        webSocketHandler.broadcastOrderMatched(update);
        log.info("Order matched notification sent: Order {}", orderId);
    }

    /**
     * Notify order cancelled
     */
    public void notifyOrderCancelled(Long orderId, Long userId, String asset,
            String side, Long quantity, String reason) {
        OrderCancelledUpdate update = new OrderCancelledUpdate();
        update.setOrderId(orderId);
        update.setUserId(userId);
        update.setAsset(asset);
        update.setSide(side);
        update.setQuantity(quantity);
        update.setReason(reason);
        update.setCancelledAt(System.currentTimeMillis());

        webSocketHandler.broadcastOrderCancelled(update);
        log.info("Order cancelled notification sent: Order {}", orderId);
    }

    /**
     * Send generic notification
     */
    public void sendNotification(Long userId, String type, String title, String message) {
        NotificationMessage notification = new NotificationMessage();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setData(new HashMap<>());
        notification.setTimestamp(System.currentTimeMillis());

        // In production, store notification in database and send via WebSocket
        log.info("Notification for user {}: {} - {}", userId, title, message);
    }

    /**
     * Send success notification
     */
    public void sendSuccessNotification(Long userId, String title, String message) {
        sendNotification(userId, "SUCCESS", title, message);
    }

    /**
     * Send error notification
     */
    public void sendErrorNotification(Long userId, String title, String message) {
        sendNotification(userId, "ERROR", title, message);
    }

    /**
     * Send warning notification
     */
    public void sendWarningNotification(Long userId, String title, String message) {
        sendNotification(userId, "WARNING", title, message);
    }

    /**
     * Send info notification
     */
    public void sendInfoNotification(Long userId, String title, String message) {
        sendNotification(userId, "INFO", title, message);
    }

    /**
     * Notify balance update
     */
    public void notifyBalanceUpdate(Long userId, Double newBalance) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("balance", newBalance);
        data.put("timestamp", System.currentTimeMillis());

        sendSuccessNotification(userId, "Balance Updated", "Your balance has been updated to " + newBalance);
    }

    /**
     * Notify order placed
     */
    public void notifyOrderPlaced(Long userId, String asset, String side, Long quantity, Double price) {
        String message = String.format("Order placed: %s %d shares of %s at $%.2f", side, quantity, asset, price);
        sendSuccessNotification(userId, "Order Placed", message);
    }

    /**
     * Notify low balance warning
     */
    public void notifyLowBalance(Long userId, Double currentBalance) {
        String message = String.format("Your account balance is low: $%.2f", currentBalance);
        sendWarningNotification(userId, "Low Balance", message);
    }

    /**
     * Notify insufficient funds
     */
    public void notifyInsufficientFunds(Long userId, Double required, Double available) {
        String message = String.format("Insufficient funds. Required: $%.2f, Available: $%.2f", required, available);
        sendErrorNotification(userId, "Insufficient Funds", message);
    }
}
