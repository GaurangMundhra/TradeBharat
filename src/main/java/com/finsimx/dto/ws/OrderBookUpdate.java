package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Order Book snapshot with depth levels
 * Contains top N bids and asks aggregated by price
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookUpdate {

    private String asset; // Asset symbol (e.g., "AAPL")
    private List<OrderBookLevel> bids; // Bid levels (highest price first)
    private List<OrderBookLevel> asks; // Ask levels (lowest price first)
    private Long timestamp; // Server timestamp

}
