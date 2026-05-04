package com.finsimx.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * User Holdings/Positions
 * Tracks what assets each user owns and their quantities
 */
@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_user_position", columnList = "user_id"),
        @Index(name = "idx_asset_position", columnList = "asset"),
        @Index(name = "idx_user_asset", columnList = "user_id, asset", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String asset;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal averageCost; // Average cost per unit

    @Column(nullable = false)
    private BigDecimal totalCost; // quantity * averageCost

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
