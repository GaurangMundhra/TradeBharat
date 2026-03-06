package com.finsimx.controller;

import com.finsimx.dto.ApiResponse;
import com.finsimx.dto.UserResponse;
import com.finsimx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile (requires authentication)
     * GET /users/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated", 401));
        }

        String username = authentication.getName();
        log.info("Fetching profile for logged-in user: {}", username);

        UserResponse userResponse = userService.getUserProfile(username);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(userResponse, "Profile fetched successfully"));
    }

    /**
     * Get specific user profile by ID (public endpoint for now)
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@PathVariable Long userId) {
        log.info("Fetching profile for user ID: {}", userId);

        UserResponse userResponse = userService.getUserProfileById(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(userResponse, "User profile fetched successfully"));
    }

    /**
     * Get user balance (requires authentication)
     * GET /users/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Object>> getUserBalance(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not authenticated", 401));
        }

        String username = authentication.getName();
        log.info("Fetching balance for logged-in user: {}", username);

        UserResponse userResponse = userService.getUserProfile(username);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(
                        java.util.Map.of("userId", userResponse.getId(), "balance", userResponse.getBalance()),
                        "Balance fetched successfully"));
    }
}
