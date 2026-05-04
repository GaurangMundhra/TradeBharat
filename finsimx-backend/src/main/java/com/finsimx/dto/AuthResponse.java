package com.finsimx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private String type;
    private Long userId;
    private String username;
    private String email;
    private java.math.BigDecimal balance;
    private String message;

    // Constructor for successful auth
    public static AuthResponse success(String token, Long userId, String username, String email,
            java.math.BigDecimal balance) {
        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(userId)
                .username(username)
                .email(email)
                .balance(balance)
                .message("Authentication successful")
                .build();
    }

    // Constructor for error response
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .message(message)
                .type("Error")
                .build();
    }
}
