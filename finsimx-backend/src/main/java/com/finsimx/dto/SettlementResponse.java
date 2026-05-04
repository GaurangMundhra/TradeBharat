package com.finsimx.dto;

import com.finsimx.entity.Settlement;
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
public class SettlementResponse {

    private Long id;
    private Long tradeId;
    private Long buyerId;
    private String buyerUsername;
    private Long sellerId;
    private String sellerUsername;
    private String asset;
    private String status; // PENDING, COMPLETED, FAILED
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal buyerCostBasis;
    private BigDecimal sellerProceeds;
    private BigDecimal sellerGainLoss;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;

    public static SettlementResponse from(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .tradeId(settlement.getTrade().getId())
                .buyerId(settlement.getBuyer().getId())
                .buyerUsername(settlement.getBuyer().getUsername())
                .sellerId(settlement.getSeller().getId())
                .sellerUsername(settlement.getSeller().getUsername())
                .asset(settlement.getAsset())
                .status(settlement.getStatus().toString())
                .quantity(settlement.getQuantityBought())
                .price(settlement.getBuyerCostBasis().divide(settlement.getQuantityBought(), 8,
                        java.math.RoundingMode.HALF_UP))
                .buyerCostBasis(settlement.getBuyerCostBasis())
                .sellerProceeds(settlement.getSellerProceeds())
                .sellerGainLoss(settlement.getSellerGainLoss())
                .createdAt(settlement.getCreatedAt())
                .settledAt(settlement.getSettledAt())
                .build();
    }
}
