package com.finsimx.controller;

import com.finsimx.dto.*;
import com.finsimx.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    /**
     * GET /api/wallet - Get wallet information for authenticated user
     * Protected: Requires valid JWT token
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getWallet(Authentication authentication) {
        log.info("GET /wallet - User: {}", authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        WalletResponse wallet = walletService.getWallet(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Wallet retrieved successfully", wallet)
        );
    }

    /**
     * GET /api/wallet/balance - Get current balance for authenticated user
     * Protected: Requires valid JWT token
     */
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getBalance(Authentication authentication) {
        log.info("GET /wallet/balance - User: {}", authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        var balance = walletService.getBalance(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Balance retrieved successfully", balance)
        );
    }

    /**
     * POST /api/wallet/deposit - Deposit funds to wallet
     * Protected: Requires valid JWT token
     * 
     * Request body:
     * {
     *   "amount": 1000.00,
     *   "description": "Monthly salary",
     *   "referenceId": "SALARY-2026-03-01"
     * }
     */
    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> deposit(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request) {
        log.info("POST /wallet/deposit - User: {} Amount: {}", authentication.getName(), request.getAmount());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        TransactionResponse transaction = walletService.deposit(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit successful", transaction));
    }

    /**
     * POST /api/wallet/withdraw - Withdraw funds from wallet
     * Protected: Requires valid JWT token
     * 
     * Request body:
     * {
     *   "amount": 500.00,
     *   "description": "Cash withdrawal",
     *   "referenceId": "WITHDRAW-2026-03-01"
     * }
     */
    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {
        log.info("POST /wallet/withdraw - User: {} Amount: {}", authentication.getName(), request.getAmount());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        TransactionResponse transaction = walletService.withdraw(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal successful", transaction));
    }

    /**
     * GET /api/wallet/transactions - Get transaction history
     * Protected: Requires valid JWT token
     * 
     * Optional parameters:
     *   - page: Page number (0-indexed)
     *   - size: Page size
     *   - sort: Sort by field (e.g., createdAt,desc)
     */
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransactions(
            Authentication authentication,
            Pageable pageable) {
        log.info("GET /wallet/transactions - User: {}", authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        Page<TransactionResponse> transactions = walletService.getTransactionHistory(userId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved successfully", transactions)
        );
    }

    /**
     * GET /api/wallet/transactions/all - Get all transactions (non-paginated)
     * Protected: Requires valid JWT token
     */
    @GetMapping("/transactions/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getAllTransactions(Authentication authentication) {
        log.info("GET /wallet/transactions/all - User: {}", authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        List<TransactionResponse> transactions = walletService.getTransactionHistory(userId);

        return ResponseEntity.ok(
                ApiResponse.success("All transactions retrieved successfully", transactions)
        );
    }

    /**
     * GET /api/wallet/transactions/type/{type} - Get transactions by type
     * Protected: Requires valid JWT token
     * 
     * Valid types: DEPOSIT, WITHDRAWAL, BUY_ORDER, SELL_ORDER, TRADE_EXECUTION, REFUND
     */
    @GetMapping("/transactions/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransactionsByType(
            Authentication authentication,
            @PathVariable String type) {
        log.info("GET /wallet/transactions/type/{} - User: {}", type, authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        List<TransactionResponse> transactions = walletService.getTransactionsByType(userId, type);

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved successfully", transactions)
        );
    }

    /**
     * GET /api/wallet/transactions/{transactionId} - Get specific transaction
     * Protected: Requires valid JWT token
     */
    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransaction(
            Authentication authentication,
            @PathVariable Long transactionId) {
        log.info("GET /wallet/transactions/{} - User: {}", transactionId, authentication.getName());

        Long userId = Long.valueOf((String) authentication.getPrincipal());
        TransactionResponse transaction = walletService.getTransaction(userId, transactionId);

        return ResponseEntity.ok(
                ApiResponse.success("Transaction retrieved successfully", transaction)
        );
    }
}
