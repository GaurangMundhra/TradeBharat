package com.finsimx.repository;

import com.finsimx.entity.User;
import com.finsimx.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Find all transactions for a user ordered by creation date descending
     */
    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find paginated transactions for a user
     */
    Page<WalletTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find transactions by user and type
     */
    List<WalletTransaction> findByUserAndTransactionTypeOrderByCreatedAtDesc(User user,
            WalletTransaction.TransactionType transactionType);

    /**
     * Find transactions within a date range
     */
    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user = :user AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findTransactionsByDateRange(@Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get total amount deposited by user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt WHERE wt.user = :user AND wt.transactionType = 'DEPOSIT'")
    java.math.BigDecimal getTotalDeposits(@Param("user") User user);

    /**
     * Get total amount withdrawn by user
     */
    @Query("SELECT COALESCE(SUM(wt.amount), 0) FROM WalletTransaction wt WHERE wt.user = :user AND wt.transactionType = 'WITHDRAWAL'")
    java.math.BigDecimal getTotalWithdrawals(@Param("user") User user);

    /**
     * Find latest transaction for a user
     */
    List<WalletTransaction> findTopByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Check if transaction with reference ID exists
     */
    boolean existsByReferenceId(String referenceId);

    /**
     * Find transaction by reference ID
     */
    WalletTransaction findByReferenceId(String referenceId);
}
