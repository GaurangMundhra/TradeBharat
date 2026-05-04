package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order cancelled update message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledUpdate {

    private Long orderId;
    private Long userId;
    private String asset;
    private String side;
    private Long quantity;
    private String reason;
    private Long cancelledAt;

}