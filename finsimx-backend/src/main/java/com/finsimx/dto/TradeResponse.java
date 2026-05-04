package com.finsimx.dto;

import com.finsimx.entity.Trade;
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
public class TradeResponse {

    private Long id;
    private Long buyOrderId;
    private Long sellOrderId;
    private Long buyerId;
    private String buyerUsername;
    private Long sellerId;
    private String sellerUsername;
    private String asset;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalValue; // price * quantity
    private LocalDateTime createdAt;

    public static TradeResponse from(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .buyerId(trade.getBuyer().getId())
                .buyerUsername(trade.getBuyer().getUsername())
                .sellerId(trade.getSeller().getId())
                .sellerUsername(trade.getSeller().getUsername())
                .asset(trade.getAsset())
                .price(trade.getPrice())
                .quantity(trade.getQuantity())
                .totalValue(trade.getPrice().multiply(trade.getQuantity()))
                .createdAt(trade.getCreatedAt())
                .build();
    }
}
