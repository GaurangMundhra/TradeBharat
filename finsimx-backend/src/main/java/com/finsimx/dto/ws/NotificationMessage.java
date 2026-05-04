package com.finsimx.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Notification message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {

    private String type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private Long timestamp;

}