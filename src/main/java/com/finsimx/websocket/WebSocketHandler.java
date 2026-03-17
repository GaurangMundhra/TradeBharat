package com.finsimx.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsimx.dto.ws.*;
import com.finsimx.service.MarketDataService;
import com.finsimx.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final MarketDataService marketDataService;
    private final ObjectMapper objectMapper;

    // Track active sessions per endpoint
    private final Map<String, Set<WebSocketSession>> sessionsByEndpoint = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String endpoint = extractEndpoint(session);
        sessionsByEndpoint.computeIfAbsent(endpoint, k -> new CopyOnWriteArraySet<>()).add(session);

        log.info("WebSocket connected: {} - Total sessions for {}: {}",
                session.getId(), endpoint, sessionsByEndpoint.getOrDefault(endpoint, new HashSet<>()).size());

        // Send initial connection confirmation
        WebSocketMessage confirmMessage = new WebSocketMessage(
                "CONNECTION_ESTABLISHED",
                new ConnectionConfirmation("Connected to " + endpoint),
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMessage)));

        // Subscribe to initial data based on endpoint
        handleSubscription(session, endpoint);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            WebSocketRequest request = objectMapper.readValue(payload, WebSocketRequest.class);

            String endpoint = extractEndpoint(session);

            switch (request.getType()) {
                case "SUBSCRIBE_PRICE" -> handlePriceSubscription(session, request, endpoint);
                case "SUBSCRIBE_POSITION" -> handlePositionSubscription(session, request, endpoint);
                case "SUBSCRIBE_PORTFOLIO" -> handlePortfolioSubscription(session, request, endpoint);
                case "UNSUBSCRIBE" -> handleUnsubscription(session, request, endpoint);
                case "GET_LATEST_PRICE" -> handleLatestPriceRequest(session, request, endpoint);
                case "PING" -> handlePing(session);
                default -> log.warn("Unknown message type: {}", request.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String endpoint = extractEndpoint(session);
        Set<WebSocketSession> sessions = sessionsByEndpoint.getOrDefault(endpoint, new HashSet<>());
        sessions.remove(session);

        if (sessions.isEmpty()) {
            sessionsByEndpoint.remove(endpoint);
        }

        log.info("WebSocket disconnected: {} - Remaining sessions for {}: {}",
                session.getId(), endpoint, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: {}", session.getId(), exception);
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // Handler methods
    private void handleSubscription(WebSocketSession session, String endpoint) throws IOException {
        if (endpoint.equals("/ws/trading")) {
            // Subscribe to trading updates
            WebSocketMessage message = new WebSocketMessage(
                    "TRADING_READY",
                    new NotificationMessage(
                            "INFO",
                            "Trading Ready",
                            "Ready to receive trade updates",
                            null,
                            System.currentTimeMillis()),
                    System.currentTimeMillis());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    private void handlePriceSubscription(WebSocketSession session, WebSocketRequest request, String endpoint)
            throws IOException {
        String asset = (String) request.getPayload().get("asset");
        log.info("User subscribed to price updates for asset: {}", asset);

        // Get latest price
        PriceUpdate priceUpdate = marketDataService.getPriceUpdate(asset);

        WebSocketMessage message = new WebSocketMessage(
                "PRICE_UPDATE",
                priceUpdate,
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void handlePositionSubscription(WebSocketSession session, WebSocketRequest request, String endpoint)
            throws IOException {
        Long userId = (Long) request.getPayload().get("userId");
        log.info("User subscribed to position updates: {}", userId);

        // Send current positions
        List<PositionUpdate> positions = marketDataService.getPositionUpdates(userId);
        for (PositionUpdate position : positions) {
            WebSocketMessage message = new WebSocketMessage(
                    "POSITION_UPDATE",
                    position,
                    System.currentTimeMillis());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    private void handlePortfolioSubscription(WebSocketSession session, WebSocketRequest request, String endpoint)
            throws IOException {
        Long userId = (Long) request.getPayload().get("userId");
        log.info("User subscribed to portfolio updates: {}", userId);

        // Send portfolio snapshot
        PortfolioUpdate portfolio = marketDataService.getPortfolioUpdate(userId);
        WebSocketMessage message = new WebSocketMessage(
                "PORTFOLIO_UPDATE",
                portfolio,
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void handleUnsubscription(WebSocketSession session, WebSocketRequest request, String endpoint)
            throws IOException {
        String subscription = (String) request.getPayload().get("subscription");
        log.info("User unsubscribed from: {}", subscription);

        WebSocketMessage message = new WebSocketMessage(
                "UNSUBSCRIBED",
                new UnsubscriptionConfirmation("Unsubscribed from " + subscription),
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void handleLatestPriceRequest(WebSocketSession session, WebSocketRequest request, String endpoint)
            throws IOException {
        String asset = (String) request.getPayload().get("asset");
        PriceUpdate priceUpdate = marketDataService.getPriceUpdate(asset);

        WebSocketMessage message = new WebSocketMessage(
                "PRICE_DATA",
                priceUpdate,
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void handlePing(WebSocketSession session) throws IOException {
        WebSocketMessage pongMessage = new WebSocketMessage(
                "PONG",
                new PingMessage("pong"),
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        WebSocketMessage errorMsg = new WebSocketMessage(
                "ERROR",
                new ErrorMessage(errorMessage),
                System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
    }

    // Broadcast methods
    public void broadcastPriceUpdate(PriceUpdate priceUpdate) {
        broadcastToEndpoint("/ws/market-data", new WebSocketMessage(
                "PRICE_UPDATE",
                priceUpdate,
                System.currentTimeMillis()));
    }

    public void broadcastTradeExecution(TradeExecutionUpdate tradeExecution) {
        broadcastToEndpoint("/ws/trading", new WebSocketMessage(
                "TRADE_EXECUTED",
                tradeExecution,
                System.currentTimeMillis()));
    }

    public void broadcastPositionUpdate(PositionUpdate positionUpdate) {
        broadcastToEndpoint("/ws/positions", new WebSocketMessage(
                "POSITION_UPDATE",
                positionUpdate,
                System.currentTimeMillis()));
    }

    public void broadcastPortfolioUpdate(PortfolioUpdate portfolioUpdate) {
        broadcastToEndpoint("/ws/portfolio", new WebSocketMessage(
                "PORTFOLIO_UPDATE",
                portfolioUpdate,
                System.currentTimeMillis()));
    }

    public void broadcastOrderMatched(OrderMatchedUpdate orderMatched) {
        broadcastToEndpoint("/ws/trading", new WebSocketMessage(
                "ORDER_MATCHED",
                orderMatched,
                System.currentTimeMillis()));
    }

    public void broadcastOrderCancelled(OrderCancelledUpdate orderCancelled) {
        broadcastToEndpoint("/ws/trading", new WebSocketMessage(
                "ORDER_CANCELLED",
                orderCancelled,
                System.currentTimeMillis()));
    }

    private void broadcastToEndpoint(String endpoint, WebSocketMessage message) {
        Set<WebSocketSession> sessions = sessionsByEndpoint.getOrDefault(endpoint, new HashSet<>());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing message", e);
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("Error sending message to session {}", session.getId(), e);
                }
            }
        }
    }

    private String extractEndpoint(WebSocketSession session) {
        String uri = session.getUri().toString();
        return uri.substring(uri.lastIndexOf("/ws"));
    }

    // Helper classes
    public static class ConnectionConfirmation {
        public String message;

        public ConnectionConfirmation(String message) {
            this.message = message;
        }
    }

    public static class UnsubscriptionConfirmation {
        public String message;

        public UnsubscriptionConfirmation(String message) {
            this.message = message;
        }
    }

    public static class ErrorMessage {
        public String error;

        public ErrorMessage(String error) {
            this.error = error;
        }
    }

    public static class PingMessage {
        public String status;

        public PingMessage(String status) {
            this.status = status;
        }
    }
}
