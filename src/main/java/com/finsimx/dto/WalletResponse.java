package com.finsimx.dto;

import com.finsimx.entity.User;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {

    private Long userId;
    private String username;
    private BigDecimal balance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private Integer transactionCount;
    private LocalDateTime lastTransactionAt;
    private LocalDateTime updatedAt;

    public static WalletResponse from(User user, BigDecimal totalDeposits, 
                                     BigDecimal totalWithdrawals, Integer transactionCount,
                                     LocalDateTime lastTransactionAt) {
        return WalletResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .balance(user.getBalance())
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .transactionCount(transactionCount)
                .lastTransactionAt(lastTransactionAt)
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
