package com.finsimx.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatsResponse {

    private Long totalOrders;
    private Long openOrders;
    private Long partialOrders;
    private Long filledOrders;
    private Long cancelledOrders;

    public Long getActiveOrders() {
        return openOrders + partialOrders;
    }

    public Long getCompletedOrders() {
        return filledOrders + cancelledOrders;
    }
}
