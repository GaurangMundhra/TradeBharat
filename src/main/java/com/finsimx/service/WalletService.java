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
import org.springframework.lang.Nullable;
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

    /**
     * Get wallet information for a user
     */
    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        log.info("Fetching wallet for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        BigDecimal totalDeposits = walletTransactionRepository.getTotalDeposits(user);
        BigDecimal totalWithdrawals = walletTransactionRepository.getTotalWithdrawals(user);

        List<WalletTransaction> transactions = walletTransactionRepository
                .findByUserOrderByCreatedAtDesc(user);

        Integer transactionCount = transactions.size();
        LocalDateTime lastTransactionAt = transactions.isEmpty() ? null : transactions.get(0).getCreatedAt();

        return WalletResponse.from(user, totalDeposits, totalWithdrawals, transactionCount, lastTransactionAt);
    }

    /**
     * Deposit funds to user wallet - ATOMIC OPERATION
     */
    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        log.info("Processing deposit for user: {} amount: {}", userId, request.getAmount());

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid deposit amount: {}", request.getAmount());
            throw new AuthException("INVALID_AMOUNT", 400, "Deposit amount must be greater than 0");
        }

        // Fetch user with pessimistic lock to prevent concurrent modifications
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        // Check for duplicate reference ID
        if (request.getReferenceId() != null && 
            walletTransactionRepository.existsByReferenceId(request.getReferenceId())) {
            log.warn("Duplicate reference ID: {}", request.getReferenceId());
            throw new AuthException("DUPLICATE_TRANSACTION", 409, "Transaction with this reference ID already exists");
        }

        // Capture current balance for transaction record
        BigDecimal balanceBefore = user.getBalance();

        // Update user balance
        BigDecimal newBalance = balanceBefore.add(request.getAmount());
        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Create and save transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Deposit to wallet")
                .referenceId(request.getReferenceId())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        log.info("Deposit successful for user: {} new balance: {}", userId, newBalance);

        return TransactionResponse.from(savedTransaction);
    }

    /**
     * Withdraw funds from user wallet - ATOMIC OPERATION
     */
    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        log.info("Processing withdrawal for user: {} amount: {}", userId, request.getAmount());

        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid withdrawal amount: {}", request.getAmount());
            throw new AuthException("INVALID_AMOUNT", 400, "Withdrawal amount must be greater than 0");
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        // Check for duplicate reference ID
        if (request.getReferenceId() != null && 
            walletTransactionRepository.existsByReferenceId(request.getReferenceId())) {
            log.warn("Duplicate reference ID: {}", request.getReferenceId());
            throw new AuthException("DUPLICATE_TRANSACTION", 409, "Transaction with this reference ID already exists");
        }

        // Check sufficient balance
        if (user.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Insufficient balance for user: {} balance: {} requested: {}", 
                    userId, user.getBalance(), request.getAmount());
            throw new AuthException("INSUFFICIENT_BALANCE", 400, 
                    "Insufficient balance. Available: " + user.getBalance());
        }

        // Capture current balance for transaction record
        BigDecimal balanceBefore = user.getBalance();

        // Update user balance
        BigDecimal newBalance = balanceBefore.subtract(request.getAmount());
        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Create and save transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal from wallet")
                .referenceId(request.getReferenceId())
                .build();

        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);
        log.info("Withdrawal successful for user: {} new balance: {}", userId, newBalance);

        return TransactionResponse.from(savedTransaction);
    }

    /**
     * Get transaction history for a user
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        log.info("Fetching transaction history for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        return walletTransactionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated transaction history
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        log.info("Fetching paginated transaction history for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        return walletTransactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(TransactionResponse::from);
    }

    /**
     * Get transactions by type
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByType(Long userId, String transactionType) {
        log.info("Fetching transactions of type: {} for user: {}", transactionType, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        try {
            WalletTransaction.TransactionType type = WalletTransaction.TransactionType.valueOf(transactionType.toUpperCase());
            return walletTransactionRepository.findByUserAndTransactionTypeOrderByCreatedAtDesc(user, type)
                    .stream()
                    .map(TransactionResponse::from)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction type: {}", transactionType);
            throw new AuthException("INVALID_TRANSACTION_TYPE", 400, 
                    "Invalid transaction type: " + transactionType);
        }
    }

    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        log.info("Fetching transaction: {} for user: {}", transactionId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        WalletTransaction transaction = walletTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new AuthException("TRANSACTION_NOT_FOUND", 404, "Transaction not found"));

        // Verify transaction belongs to user
        if (!transaction.getUser().getId().equals(userId)) {
            log.warn("User: {} attempted to access transaction: {} which belongs to user: {}", 
                    userId, transactionId, transaction.getUser().getId());
            throw new AuthException("UNAUTHORIZED", 403, "Cannot access this transaction");
        }

        return TransactionResponse.from(transaction);
    }

    /**
     * Check user balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        log.info("Fetching balance for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        return user.getBalance();
    }

    /**
     * Deduct balance for order placement - INTERNAL USE
     */
    @Transactional
    public void deductBalanceForOrder(Long userId, BigDecimal amount, String referenceId, String description) {
        log.info("Deducting balance for user: {} amount: {} reference: {}", userId, amount, referenceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        if (user.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance for order - user: {} balance: {} needed: {}", 
                    userId, user.getBalance(), amount);
            throw new AuthException("INSUFFICIENT_BALANCE", 400, "Insufficient balance for order");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal newBalance = balanceBefore.subtract(amount);

        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.BUY_ORDER)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(description != null ? description : "Buy order deduction")
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);
    }

    /**
     * Credit balance for trade execution - INTERNAL USE
     */
    @Transactional
    public void creditBalanceForTrade(Long userId, BigDecimal amount, String referenceId, String description) {
        log.info("Crediting balance for user: {} amount: {} reference: {}", userId, amount, referenceId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", 404, "User not found"));

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal newBalance = balanceBefore.add(amount);

        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .transactionType(WalletTransaction.TransactionType.TRADE_EXECUTION)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(newBalance)
                .description(description != null ? description : "Trade execution credit")
                .referenceId(referenceId)
                .build();

        walletTransactionRepository.save(transaction);
    }
}
