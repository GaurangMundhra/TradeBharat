package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Portfolio update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdate {

    private Long userId;
    private String userName;
    private Double totalValue;
    private Double cashBalance;
    private Double investedValue;
    private Double totalUnrealizedPnL;
    private Double totalUnrealizedPnLPercent;
    private Integer positionCount;
    private Long timestamp;

}