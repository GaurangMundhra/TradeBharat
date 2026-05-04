package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single price level in order book
 * Represents aggregated quantity at a specific price point
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookLevel {

    private Double price; // Price level
    private Double quantity; // Total quantity at this price level

}
