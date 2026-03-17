package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Price update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdate {

    private String asset;
    private Double currentPrice;
    private Double previousPrice;
    private Double change;
    private Double changePercent;
    private Long timestamp;
    private Long volume;

}