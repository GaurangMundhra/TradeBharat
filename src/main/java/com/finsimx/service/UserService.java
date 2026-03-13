package com.finsimx.service;

import com.finsimx.dto.AuthResponse;
import com.finsimx.dto.LoginRequest;
import com.finsimx.dto.RegisterRequest;
import com.finsimx.dto.UserResponse;
import com.finsimx.entity.User;
import com.finsimx.exception.InvalidCredentialsException;
import com.finsimx.exception.UserAlreadyExistsException;
import com.finsimx.exception.UserNotFoundException;
import com.finsimx.repository.UserRepository;
import com.finsimx.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());

        // Validate passwords match
        if (!registerRequest.passwordsMatch()) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Username already exists: {}", registerRequest.getUsername());
            throw new UserAlreadyExistsException("Username already taken: " + registerRequest.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Email already exists: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("Email already registered: " + registerRequest.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .balance(BigDecimal.valueOf(100000)) // Initial balance
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {} with ID: {}", savedUser.getUsername(), savedUser.getId());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole());

        return AuthResponse.success(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getBalance());
    }

    /**
     * Login user with username and password
     */
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        // Find user by username
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> {
                    log.warn("User not found: {}", loginRequest.getUsername());
                    return new UserNotFoundException("User not found: " + loginRequest.getUsername());
                });

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", loginRequest.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        log.info("User logged in successfully: {}", user.getUsername());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole());

        return AuthResponse.success(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getBalance());
    }

    /**
     * Get user profile by username
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(String username) {
        log.debug("Fetching profile for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return UserResponse.fromUser(user);
    }

    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfileById(Long userId) {
        log.debug("Fetching profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return UserResponse.fromUser(user);
    }

    /**
     * Get balance for a user
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserBalance(Long userId) {
        log.debug("Fetching balance for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return user.getBalance();
    }

    /**
     * Update user balance (used internally by wallet service)
     */
    public void updateBalance(Long userId, BigDecimal newBalance) {
        log.info("Updating balance for user ID: {} to: {}", userId, newBalance);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Check if user exists by username
     */
    @Transactional(readOnly = true)
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if user exists by ID
     */
    @Transactional(readOnly = true)
    public boolean userExistsById(Long userId) {
        return userRepository.existsById(userId);
    }

    /**
     * Get user by ID (internal use)
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
