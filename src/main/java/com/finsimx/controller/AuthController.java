package com.finsimx.controller;

import com.finsimx.dto.ApiResponse;
import com.finsimx.dto.LoginRequest;
import com.finsimx.dto.RegisterRequest;
import com.finsimx.dto.AuthResponse;
import com.finsimx.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * Register a new user
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Received registration request for user: {}", registerRequest.getUsername());

        AuthResponse response = userService.register(registerRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    /**
     * Login user
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("Received login request for user: {}", loginRequest.getUsername());

        AuthResponse response = userService.login(loginRequest);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(response, "User logged in successfully"));
    }

    /**
     * Health check endpoint
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is healthy", "Health check passed"));
    }
}
