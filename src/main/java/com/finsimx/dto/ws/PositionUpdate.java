package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Position update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionUpdate {

    private Long userId;
    private String asset;
    private Long quantity;
    private Long longQuantity;
    private Long shortQuantity;
    private Double currentPrice;
    private Double value;
    private Double unrealizedPnL;
    private Double unrealizedPnLPercent;
    private Long timestamp;

}