package com.finsimx.dto;

import com.finsimx.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionResponse {

    private Long id;
    private String asset;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal totalCost;
    private BigDecimal currentValue; // Will be calculated if market price provided
    private BigDecimal unrealizedPnL; // currentValue - totalCost
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PositionResponse from(Position position) {
        return PositionResponse.builder()
                .id(position.getId())
                .asset(position.getAsset())
                .quantity(position.getQuantity())
                .averageCost(position.getAverageCost())
                .totalCost(position.getTotalCost())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }

    public static PositionResponse from(Position position, BigDecimal currentPrice) {
        PositionResponse response = from(position);
        if (currentPrice != null) {
            response.setCurrentValue(currentPrice.multiply(position.getQuantity()));
            response.setUnrealizedPnL(response.getCurrentValue().subtract(position.getTotalCost()));
        }
        return response;
    }
}
