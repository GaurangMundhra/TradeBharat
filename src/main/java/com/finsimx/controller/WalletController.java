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
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final WalletService walletService;

    /**
     * Helper method to extract username from JWT
     */
    private String getUsername(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * GET /api/wallet
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getWallet(Authentication authentication) {

        String username = getUsername(authentication);
        log.info("GET /wallet - User: {}", username);

        WalletResponse wallet = walletService.getWalletByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success("Wallet retrieved successfully", wallet));
    }

    /**
     * GET /api/wallet/balance
     */
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getBalance(Authentication authentication) {

        String username = getUsername(authentication);
        log.info("GET /wallet/balance - User: {}", username);

        var balance = walletService.getBalanceByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success("Balance retrieved successfully", balance));
    }

    /**
     * POST /api/wallet/deposit
     */
    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> deposit(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request) {

        String username = getUsername(authentication);
        log.info("POST /wallet/deposit - User: {} Amount: {}", username, request.getAmount());

        TransactionResponse transaction = walletService.depositByUsername(username, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Deposit successful", transaction));
    }

    /**
     * POST /api/wallet/withdraw
     */
    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawRequest request) {

        String username = getUsername(authentication);
        log.info("POST /wallet/withdraw - User: {} Amount: {}", username, request.getAmount());

        TransactionResponse transaction = walletService.withdrawByUsername(username, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Withdrawal successful", transaction));
    }

    /**
     * GET /api/wallet/transactions
     */
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransactions(
            Authentication authentication,
            Pageable pageable) {

        String username = getUsername(authentication);
        log.info("GET /wallet/transactions - User: {}", username);

        Page<TransactionResponse> transactions = walletService.getTransactionHistoryByUsername(username, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved successfully", transactions));
    }

    /**
     * GET /api/wallet/transactions/all
     */
    @GetMapping("/transactions/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getAllTransactions(Authentication authentication) {

        String username = getUsername(authentication);
        log.info("GET /wallet/transactions/all - User: {}", username);

        List<TransactionResponse> transactions = walletService.getTransactionHistoryByUsername(username);

        return ResponseEntity.ok(
                ApiResponse.success("All transactions retrieved successfully", transactions));
    }

    /**
     * GET /api/wallet/transactions/type/{type}
     */
    @GetMapping("/transactions/type/{type}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransactionsByType(
            Authentication authentication,
            @PathVariable String type) {

        String username = getUsername(authentication);
        log.info("GET /wallet/transactions/type/{} - User: {}", type, username);

        List<TransactionResponse> transactions = walletService.getTransactionsByTypeByUsername(username, type);

        return ResponseEntity.ok(
                ApiResponse.success("Transactions retrieved successfully", transactions));
    }

    /**
     * GET /api/wallet/transactions/{transactionId}
     */
    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTransaction(
            Authentication authentication,
            @PathVariable Long transactionId) {

        String username = getUsername(authentication);
        log.info("GET /wallet/transactions/{} - User: {}", transactionId, username);

        TransactionResponse transaction = walletService.getTransactionByUsername(username, transactionId);

        return ResponseEntity.ok(
                ApiResponse.success("Transaction retrieved successfully", transaction));
    }
}