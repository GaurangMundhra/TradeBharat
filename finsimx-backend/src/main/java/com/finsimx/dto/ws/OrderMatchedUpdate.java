package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order matched update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMatchedUpdate {

    private Long orderId;
    private Long tradeId;
    private String asset;
    private String side;
    private Long quantity;
    private Double price;
    private Double totalValue;
    private String status;
    private Long timestamp;

}