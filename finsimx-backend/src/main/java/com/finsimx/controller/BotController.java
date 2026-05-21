package com.finsimx.controller;

import com.finsimx.dto.ApiResponse;
import com.finsimx.service.MarketBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Bot Admin Controller
 * Endpoints to control the market maker bot.
 */
@RestController
@RequestMapping("/admin/bot")
@RequiredArgsConstructor
@Slf4j
public class BotController {

    private final MarketBotService marketBotService;

    /**
     * GET /api/admin/bot/status
     * Get bot status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<?>> getStatus() {
        return ResponseEntity.ok(
                ApiResponse.success("Bot status retrieved", marketBotService.getStatus()));
    }

    /**
     * POST /api/admin/bot/toggle
     * Enable or disable the bot
     */
    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse<?>> toggle(@RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", !marketBotService.isEnabled());
        marketBotService.setEnabled(enabled);
        log.info("Market bot {} by admin", enabled ? "ENABLED" : "DISABLED");
        return ResponseEntity.ok(
                ApiResponse.success("Bot " + (enabled ? "enabled" : "disabled"), marketBotService.getStatus()));
    }

    /**
     * PUT /api/admin/bot/config
     * Update bot parameters
     */
    @PutMapping("/config")
    public ResponseEntity<ApiResponse<?>> updateConfig(@RequestBody Map<String, Double> body) {
        if (body.containsKey("spreadPercent")) {
            marketBotService.setSpreadPercent(body.get("spreadPercent"));
        }
        if (body.containsKey("volatility")) {
            marketBotService.setVolatility(body.get("volatility"));
        }
        log.info("Bot config updated: {}", body);
        return ResponseEntity.ok(
                ApiResponse.success("Bot config updated", marketBotService.getStatus()));
    }
}
