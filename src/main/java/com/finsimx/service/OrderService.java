package com.finsimx.service;

import com.finsimx.dto.CreateOrderRequest;
import com.finsimx.dto.OrderResponse;
import com.finsimx.dto.OrderStatsResponse;
import com.finsimx.entity.Order;
import com.finsimx.entity.User;
import com.finsimx.exception.*;
import com.finsimx.repository.OrderRepository;
import com.finsimx.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final MatchingService matchingService;

    /*
     * ------------------------------------------------------------
     * Helper Methods
     * ------------------------------------------------------------
     */

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));
    }

    /*
     * ------------------------------------------------------------
     * Username based methods (called by controller)
     * ------------------------------------------------------------
     */

    @Transactional
    public OrderResponse placeOrder(String username, CreateOrderRequest request) {
        User user = getUserByUsername(username);
        return placeOrder(user.getId(), request);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String username, Long orderId) {

        User user = getUserByUsername(username);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Ensure the order belongs to the authenticated user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new OrderException("UNAUTHORIZED", 403, "Cannot access this order");
        }

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(String username, Pageable pageable) {
        User user = getUserByUsername(username);
        return getUserOrders(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllUserOrders(String username) {
        User user = getUserByUsername(username);
        return getAllUserOrders(user.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(String username, String status) {
        User user = getUserByUsername(username);
        return getOrdersByStatus(user.getId(), status);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByType(String username, String type) {
        User user = getUserByUsername(username);
        return getOrdersByType(user.getId(), type);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders(String username) {
        User user = getUserByUsername(username);
        return getActiveOrders(user.getId());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByAsset(String username, String asset) {
        User user = getUserByUsername(username);
        return getOrdersByAsset(user.getId(), asset);
    }

    @Transactional
    public OrderResponse cancelOrder(String username, Long orderId) {
        User user = getUserByUsername(username);
        return cancelOrder(user.getId(), orderId);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats(String username) {
        User user = getUserByUsername(username);
        return getOrderStats(user.getId());
    }

    /*
     * ------------------------------------------------------------
     * Core Order Logic (existing code)
     * ------------------------------------------------------------
     */

    @Transactional
    public OrderResponse placeOrder(Long userId, CreateOrderRequest request) {

        log.info("Placing order user:{} asset:{} qty:{} price:{}",
                userId, request.getAsset(), request.getQuantity(), request.getPrice());

        User user = getUser(userId);

        if (request.getAsset() == null || request.getAsset().trim().isEmpty()) {
            throw new OrderException("INVALID_ASSET", 400, "Asset cannot be empty");
        }

        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("INVALID_PRICE", 400, "Price must be greater than 0");
        }

        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderException("INVALID_QUANTITY", 400, "Quantity must be greater than 0");
        }

        Order.OrderType orderType = Order.OrderType.valueOf(request.getType().toUpperCase());

        BigDecimal orderValue = request.getPrice().multiply(request.getQuantity());

        if (orderType == Order.OrderType.BUY) {

            BigDecimal balance = walletService.getBalance(userId);

            if (balance.compareTo(orderValue) < 0) {
                throw new InsufficientBalanceForOrderException(
                        "Required: " + orderValue + " Available: " + balance);
            }

            String referenceId = "ORDER-BUY-" + userId + "-" + System.currentTimeMillis();

            walletService.deductBalanceForOrder(
                    userId,
                    orderValue,
                    referenceId,
                    "Buy order " + request.getAsset());
        }

        Order order = Order.builder()
                .user(user)
                .asset(request.getAsset().toUpperCase())
                .type(orderType)
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .filledQuantity(BigDecimal.ZERO)
                .status(Order.OrderStatus.OPEN)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order {} created: {} {} @ {} qty={}", 
            saved.getId(), orderType, saved.getAsset(), saved.getPrice(), saved.getQuantity());

        // Trigger order matching
        try {
            var trades = matchingService.matchOrder(saved);
            if (!trades.isEmpty()) {
                log.info("Order {} matched with {} trades", saved.getId(), trades.size());
                // Reload order to get updated filled quantity
                saved = orderRepository.findById(saved.getId()).orElse(saved);
            }
        } catch (Exception e) {
            log.warn("Matching failed for order {}: {}", saved.getId(), e.getMessage());
            // Don't fail the order placement if matching fails
        }

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {

        User user = getUser(userId);

        return orderRepository
                .findByUserOrderByCreatedAtDesc(user, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllUserOrders(Long userId) {

        User user = getUser(userId);

        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(Long userId, String status) {

        User user = getUser(userId);

        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());

        return orderRepository
                .findByUserAndStatusOrderByCreatedAtDesc(user, orderStatus)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByType(Long userId, String type) {

        User user = getUser(userId);

        Order.OrderType orderType = Order.OrderType.valueOf(type.toUpperCase());

        return orderRepository
                .findByUserAndTypeOrderByCreatedAtDesc(user, orderType)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders(Long userId) {

        User user = getUser(userId);

        List<Order> open = orderRepository.findByUserAndStatusOrderByCreatedAtDesc(
                user, Order.OrderStatus.OPEN);

        List<Order> partial = orderRepository.findByUserAndStatusOrderByCreatedAtDesc(
                user, Order.OrderStatus.PARTIAL);

        open.addAll(partial);

        return open.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId) {

        User user = getUser(userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new OrderException("UNAUTHORIZED", 403, "Cannot cancel order");
        }

        if (order.getStatus() == Order.OrderStatus.FILLED) {
            throw new OrderException("CANNOT_CANCEL_FILLED", 400, "Cannot cancel filled order");
        }

        if (order.getType() == Order.OrderType.BUY) {

            BigDecimal refund = order.getPrice().multiply(
                    order.getQuantity().subtract(order.getFilledQuantity()));

            if (refund.compareTo(BigDecimal.ZERO) > 0) {

                walletService.creditBalanceForTrade(
                        userId,
                        refund,
                        "REFUND-" + orderId,
                        "Cancelled order refund");
            }
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        // Remove order from matching order book
        try {
            matchingService.processCancelledOrder(saved);
            log.info("Order {} removed from order book", orderId);
        } catch (Exception e) {
            log.warn("Failed to remove order from book: {}", e.getMessage());
            // Don't fail the cancellation if removal from book fails
        }

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getOrderStats(Long userId) {

        User user = getUser(userId);

        return OrderStatsResponse.builder()
                .totalOrders(orderRepository.countByUser(user))
                .openOrders(orderRepository.countByUserAndStatus(user, Order.OrderStatus.OPEN))
                .partialOrders(orderRepository.countByUserAndStatus(user, Order.OrderStatus.PARTIAL))
                .filledOrders(orderRepository.countByUserAndStatus(user, Order.OrderStatus.FILLED))
                .cancelledOrders(orderRepository.countByUserAndStatus(user, Order.OrderStatus.CANCELLED))
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByAsset(Long userId, String asset) {

        User user = getUser(userId);

        return orderRepository
                .findByUserAndAssetOrderByCreatedAtDesc(user, asset.toUpperCase())
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

}