package com.finsimx.dto;

import com.finsimx.entity.Order;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private String username;
    private String asset;
    private String type;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private BigDecimal remainingQuantity;
    private String status;
    private BigDecimal totalValue;
    private BigDecimal filledValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        BigDecimal remainingQuantity = order.getQuantity().subtract(order.getFilledQuantity());
        BigDecimal totalValue = order.getPrice().multiply(order.getQuantity());
        BigDecimal filledValue = order.getPrice().multiply(order.getFilledQuantity());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .asset(order.getAsset())
                .type(order.getType().name())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .filledQuantity(order.getFilledQuantity())
                .remainingQuantity(remainingQuantity)
                .status(order.getStatus().name())
                .totalValue(totalValue)
                .filledValue(filledValue)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
