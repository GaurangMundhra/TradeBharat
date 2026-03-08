package com.finsimx.controller;

import com.finsimx.dto.*;
import com.finsimx.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    private String getUsername(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * POST /api/orders
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> placeOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        String username = getUsername(authentication);

        log.info("POST /orders - User: {} Type: {} Asset: {}",
                username, request.getType(), request.getAsset());

        OrderResponse order = orderService.placeOrder(username, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    /**
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrder(
            Authentication authentication,
            @PathVariable Long orderId) {

        String username = getUsername(authentication);

        log.info("GET /orders/{} - User: {}", orderId, username);

        OrderResponse order = orderService.getOrder(username, orderId);

        return ResponseEntity.ok(
                ApiResponse.success("Order retrieved successfully", order));
    }

    /**
     * GET /api/orders
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getUserOrders(
            Authentication authentication,
            Pageable pageable) {

        String username = getUsername(authentication);

        log.info("GET /orders - User: {}", username);

        Page<OrderResponse> orders = orderService.getUserOrders(username, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/list/all
     */
    @GetMapping("/list/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getAllUserOrders(
            Authentication authentication) {

        String username = getUsername(authentication);

        log.info("GET /orders/list/all - User: {}", username);

        List<OrderResponse> orders = orderService.getAllUserOrders(username);

        return ResponseEntity.ok(
                ApiResponse.success("All orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrdersByStatus(
            Authentication authentication,
            @PathVariable String status) {

        String username = getUsername(authentication);

        log.info("GET /orders/status/{} - User: {}", status, username);

        List<OrderResponse> orders = orderService.getOrdersByStatus(username, status);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/type/{type}
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrdersByType(
            Authentication authentication,
            @PathVariable String type) {

        String username = getUsername(authentication);

        log.info("GET /orders/type/{} - User: {}", type, username);

        List<OrderResponse> orders = orderService.getOrdersByType(username, type);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/active
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getActiveOrders(
            Authentication authentication) {

        String username = getUsername(authentication);

        log.info("GET /orders/active - User: {}", username);

        List<OrderResponse> orders = orderService.getActiveOrders(username);

        return ResponseEntity.ok(
                ApiResponse.success("Active orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/asset/{asset}
     */
    @GetMapping("/asset/{asset}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrdersByAsset(
            Authentication authentication,
            @PathVariable String asset) {

        String username = getUsername(authentication);

        log.info("GET /orders/asset/{} - User: {}", asset, username);

        List<OrderResponse> orders = orderService.getOrdersByAsset(username, asset);

        return ResponseEntity.ok(
                ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * GET /api/orders/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getOrderStats(
            Authentication authentication) {

        String username = getUsername(authentication);

        log.info("GET /orders/stats - User: {}", username);

        OrderStatsResponse stats = orderService.getOrderStats(username);

        return ResponseEntity.ok(
                ApiResponse.success("Order statistics retrieved successfully", stats));
    }

    /**
     * DELETE /api/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId) {

        String username = getUsername(authentication);

        log.info("DELETE /orders/{} - User: {}", orderId, username);

        OrderResponse order = orderService.cancelOrder(username, orderId);

        return ResponseEntity.ok(
                ApiResponse.success("Order cancelled successfully", order));
    }
}