package com.finsimx.dto;

import com.finsimx.entity.WalletTransaction;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private Long userId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceId;
    private LocalDateTime createdAt;

    public static TransactionResponse from(WalletTransaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUser().getId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
