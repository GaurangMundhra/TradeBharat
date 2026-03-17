package com.finsimx.config;

import com.finsimx.websocket.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(webSocketHandler, "/ws/trading")
                .setAllowedOrigins("*");

        registry.addHandler(webSocketHandler, "/ws/market-data")
                .setAllowedOrigins("*");

        registry.addHandler(webSocketHandler, "/ws/positions")
                .setAllowedOrigins("*");

        registry.addHandler(webSocketHandler, "/ws/portfolio")
                .setAllowedOrigins("*");
    }
}
