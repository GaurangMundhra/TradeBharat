package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Main WebSocket message wrapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private String type; // MESSAGE_TYPE (e.g., PRICE_UPDATE)
    private Object payload; // Actual data
    private Long timestamp; // Server timestamp

}