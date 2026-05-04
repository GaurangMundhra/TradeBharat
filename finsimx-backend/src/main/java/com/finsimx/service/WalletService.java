package com.finsimx.service;

import com.finsimx.dto.*;
import com.finsimx.entity.User;
import com.finsimx.entity.WalletTransaction;
import com.finsimx.exception.AuthException;
import com.finsimx.repository.UserRepository;
import com.finsimx.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /*
     * ------------------------------------------------------------
     * Helper Methods
     * ------------------------------------------------------------
     */

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));
    }

    /*
     * ------------------------------------------------------------
     * Username Based APIs (Used by Controller)
     * ------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUsername(String username) {
        User user = getUserByUsername(username);
        return getWallet(user.getId());
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalanceByUsername(String username) {
        User user = getUserByUsername(username);
        return user.getBalance();
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {

        log.info("Fetching balance for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        return user.getBalance();
    }

    @Transactional
    public TransactionResponse depositByUsername(String username, DepositRequest request) {
        User user = getUserByUsername(username);
        return deposit(user.getId(), request);
    }

    @Transactional
    public TransactionResponse withdrawByUsername(String username, WithdrawRequest request) {
        User user = getUserByUsername(username);
        return withdraw(user.getId(), request);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistoryByUsername(String username) {
        User user = getUserByUsername(username);
        return getTransactionHistory(user.getId());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistoryByUsername(String username, Pageable pageable) {
        User user = getUserByUsername(username);
        return getTransactionHistory(user.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByTypeByUsername(String username, String type) {
        User user = getUserByUsername(username);
        return getTransactionsByType(user.getId(), type);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByUsername(String username, Long transactionId) {
        User user = getUserByUsername(username);
        return getTransaction(user.getId(), transactionId);
    }

    /*
     * ------------------------------------------------------------
     * Core Wallet Logic
     * ------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {

        log.info("Fetching wallet for user {}", userId);

        User user = getUser(userId);

        BigDecimal totalDeposits = walletTransactionRepository.getTotalDeposits(user);
        BigDecimal totalWithdrawals = walletTransactionRepository.getTotalWithdrawals(user);

        List<WalletTransaction> transactions = walletTransactionRepository.findByUserOrderByCreatedAtDesc(user);

        Integer transactionCount = transactions.size();

        LocalDateTime lastTransactionAt = transactions.isEmpty() ? null : transactions.get(0).getCreatedAt();

        return WalletResponse.from(user, totalDeposits, totalWithdrawals, transactionCount, lastTransactionAt);
    }

    /*
     * ------------------------------------------------------------
     * Deposit
     * ------------------------------------------------------------
     */

    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {

        log.info("Deposit request user {} amount {}", userId, request.getAmount());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuthException("INVALID_AMOUNT", 400, "Deposit amount must be greater than 0");
        }

        User user = getUser(userId);

        if (request.getReferenceId() != null &&
                walletTransactionRepository.existsByReferenceId(request.getReferenceId())) {
            throw new AuthException("DUPLICATE_TRANSACTION", 409, "Transaction already exists");
        }

        BigDecimal balanceBefore = user.getBalance();

        BigDecimal newBalance = balanceBefore.add(request.getAmount());

        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .referenceId(request.getReferenceId())
                .build();

        WalletTransaction saved = walletTransactionRepository.save(transaction);

        log.info("Deposit successful new balance {}", newBalance);

        return TransactionResponse.from(saved);
    }

    /*
     * ------------------------------------------------------------
     * Withdraw
     * ------------------------------------------------------------
     */

    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {

        log.info("Withdraw request user {} amount {}", userId, request.getAmount());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AuthException("INVALID_AMOUNT", 400, "Withdrawal amount must be greater than 0");
        }

        User user = getUser(userId);

        if (request.getReferenceId() != null &&
                walletTransactionRepository.existsByReferenceId(request.getReferenceId())) {
            throw new AuthException("DUPLICATE_TRANSACTION", 409, "Transaction already exists");
        }

        if (user.getBalance().compareTo(request.getAmount()) < 0) {
            throw new AuthException("INSUFFICIENT_BALANCE", 400, "Insufficient balance");
        }

        BigDecimal balanceBefore = user.getBalance();

        BigDecimal newBalance = balanceBefore.subtract(request.getAmount());

        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .referenceId(request.getReferenceId())
                .build();

        WalletTransaction saved = walletTransactionRepository.save(transaction);

        return TransactionResponse.from(saved);
    }

    /*
     * ------------------------------------------------------------
     * Transaction History
     * ------------------------------------------------------------
     */

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long userId) {

        User user = getUser(userId);

        return walletTransactionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {

        User user = getUser(userId);

        return walletTransactionRepository
                .findByUserOrderByCreatedAtDesc(user, pageable)
                .map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByType(Long userId, String type) {

        User user = getUser(userId);

        try {

            WalletTransaction.TransactionType transactionType = WalletTransaction.TransactionType
                    .valueOf(type.toUpperCase());

            return walletTransactionRepository
                    .findByUserAndTransactionTypeOrderByCreatedAtDesc(user, transactionType)
                    .stream()
                    .map(TransactionResponse::from)
                    .collect(Collectors.toList());

        } catch (IllegalArgumentException e) {

            throw new AuthException("INVALID_TRANSACTION_TYPE", 400, "Invalid transaction type");
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {

        User user = getUser(userId);

        WalletTransaction transaction = walletTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AuthException("TRANSACTION_NOT_FOUND", 404, "Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new AuthException("UNAUTHORIZED", 403, "Cannot access this transaction");
        }

        return TransactionResponse.from(transaction);
    }

    /*
     * ------------------------------------------------------------
     * Order Engine Integration
     * ------------------------------------------------------------
     */

    @Transactional
    public void deductBalanceForOrder(Long userId, BigDecimal amount, String referenceId, String description) {

        User user = getUser(userId);

        if (user.getBalance().compareTo(amount) < 0) {
            throw new AuthException("INSUFFICIENT_BALANCE", 400, "Insufficient balance for order");
        }

        BigDecimal before = user.getBalance();

        BigDecimal after = before.subtract(amount);

        user.setBalance(after);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.BUY_ORDER)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(description != null ? description : "Buy order deduction")
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);
    }

    @Transactional
    public void creditBalanceForTrade(Long userId, BigDecimal amount, String referenceId, String description) {

        User user = getUser(userId);

        BigDecimal before = user.getBalance();

        BigDecimal after = before.add(amount);

        user.setBalance(after);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.TRADE_EXECUTION)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(description != null ? description : "Trade execution credit")
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);
    }
}