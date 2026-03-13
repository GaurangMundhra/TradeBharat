package com.finsimx.controller;

import com.finsimx.dto.PortfolioResponse;
import com.finsimx.dto.PositionResponse;
import com.finsimx.dto.SettlementResponse;
import com.finsimx.service.MatchingService;
import com.finsimx.service.TradeExecutionService;
import com.finsimx.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.finsimx.repository.UserRepository;
import com.finsimx.entity.User;
import java.util.List;

@RestController
@RequestMapping("/settlements")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {

    private final UserRepository userRepository;
    private final TradeExecutionService tradeExecutionService;
    private final MatchingService matchingService;

    /**
     * POST /api/settlements/execute/{tradeId}
     * Execute/settle a trade
     */
    @PostMapping("/execute/{tradeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> executeTrade(@PathVariable Long tradeId) {
        log.info("Executing trade {}", tradeId);

        SettlementResponse settlement = tradeExecutionService.executeTrade(tradeId);
        return ResponseEntity.ok(ApiResponse.success("Trade executed successfully", settlement));
    }

    /**
     * POST /api/settlements/execute-all
     * Execute all pending trades
     */
    @PostMapping("/execute-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> executeAllPending() {
        log.info("Executing all pending trades");

        List<SettlementResponse> settlements = tradeExecutionService.executeAllPendingTrades();
        return ResponseEntity.ok(ApiResponse.success("Executed " + settlements.size() + " trades", settlements));
    }

    /**
     * GET /api/settlements/{settlementId}
     * Get settlement details
     */
    @GetMapping("/{settlementId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getSettlement(@PathVariable Long settlementId) {
        SettlementResponse settlement = tradeExecutionService.getSettlement(settlementId);
        return ResponseEntity.ok(ApiResponse.success("Settlement retrieved successfully", settlement));
    }

    /**
     * GET /api/settlements/user/me
     * Get authenticated user's settlements
     */
    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getUserSettlements(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        Long userId = Long.valueOf(username.split(":")[0]);

        List<SettlementResponse> settlements = tradeExecutionService.getUserSettlements(userId);
        return ResponseEntity.ok(ApiResponse.success("User settlements retrieved successfully", settlements));
    }

    /**
     * GET /api/settlements/pending
     * Get all pending settlements (admin only)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPending() {
        List<SettlementResponse> settlements = tradeExecutionService.getPendingSettlements();
        return ResponseEntity.ok(ApiResponse.success("Pending settlements retrieved successfully", settlements));
    }

    /**
     * GET /api/positions/me
     * Get authenticated user's positions
     */
    @GetMapping("/positions/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getUserPositions(Authentication authentication) {
        String username = (String) authentication.getPrincipal();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();

        List<PositionResponse> positions = tradeExecutionService.getUserPositions(userId);
        return ResponseEntity.ok(ApiResponse.success("User positions retrieved successfully", positions));
    }

    /**
     * GET /api/positions/{asset}/me
     * Get user's position in specific asset
     */
    @GetMapping("/positions/{asset}/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getPosition(
            @PathVariable String asset,
            Authentication authentication) {

        String username = (String) authentication.getPrincipal();
        Long userId = Long.valueOf(username.split(":")[0]);

        PositionResponse position = tradeExecutionService.getPosition(userId, asset.toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Position retrieved successfully", position));
    }

    /**
     * GET /api/portfolio/me
     * Get complete portfolio with positions, values, and P&L
     */
    @GetMapping("/portfolio/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getPortfolio(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        Long userId = Long.valueOf(username.split(":")[0]);

        PortfolioResponse portfolio = tradeExecutionService.getPortfolio(userId, matchingService);
        return ResponseEntity.ok(ApiResponse.success("Portfolio retrieved successfully", portfolio));
    }

    /**
     * GET /api/pnl/realized/me
     * Get realized gains and losses for user
     */
    @GetMapping("/pnl/realized/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getRealizedPnL(Authentication authentication) {
        String username = (String) authentication.getPrincipal();
        Long userId = Long.valueOf(username.split(":")[0]);

        var pnl = tradeExecutionService.getRealizingGainLoss(userId);
        return ResponseEntity.ok(ApiResponse.success("Realized P&L retrieved successfully",
                java.util.Map.of("realizedPnL", pnl)));
    }
}
