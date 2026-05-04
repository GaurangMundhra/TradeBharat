package com.finsimx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {

    private Long userId;
    private String username;
    private BigDecimal cashBalance;
    private BigDecimal portfolioValue; // Total value of all positions
    private BigDecimal unrealizedPnL; // Total unrealized gain/loss
    private BigDecimal totalInvested; // Total cost basis
    private Integer positionCount;
    private java.util.List<PositionResponse> positions;

    public BigDecimal getTotalValue() {
        if (portfolioValue != null && cashBalance != null) {
            return portfolioValue.add(cashBalance);
        }
        return null;
    }
}
