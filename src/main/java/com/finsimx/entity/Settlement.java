package com.finsimx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trade Settlement Record
 * Tracks the settlement/execution of each trade
 */
@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_trade_settlement", columnList = "trade_id"),
        @Index(name = "idx_buyer_settlement", columnList = "buyer_id"),
        @Index(name = "idx_seller_settlement", columnList = "seller_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_settled_at", columnList = "settled_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false, unique = true)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String asset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status; // PENDING, COMPLETED, FAILED

    // Buyer side
    @Column(nullable = false)
    private BigDecimal buyerCostBasis; // Total amount paid

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "total_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalValue;

    @Column(nullable = false)
    private BigDecimal quantityBought;

    // Seller side
    @Column(nullable = false)
    private BigDecimal sellerProceeds; // Total amount received
    @Column(nullable = false)
    private BigDecimal quantitySold;
    @Column
    private BigDecimal sellerCostBasis; // Original cost to seller (for P&L)
    @Column
    private BigDecimal sellerGainLoss; // sellerProceeds - sellerCostBasis
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime settledAt;

    @Column
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum SettlementStatus {
        PENDING, COMPLETED, FAILED
    }
}
