package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trade execution update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutionUpdate {

    private Long tradeId;
    private Long buyerId;
    private Long sellerId;
    private String buyerName;
    private String sellerName;
    private String asset;
    private Long quantity;
    private Double price;
    private Double totalValue;
    private String status;
    private Long executedAt;

}