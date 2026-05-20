package com.solarride.solarride.service.auth;

import com.solarride.solarride.domain.user.User;
import com.solarride.solarride.domain.user.UserStatus;
import com.solarride.solarride.dto.request.LoginRequest;
import com.solarride.solarride.dto.request.OtpVerifyRequest;
import com.solarride.solarride.dto.request.RefreshTokenRequest;
import com.solarride.solarride.dto.response.AuthResponse;
import com.solarride.solarride.dto.response.OtpResponse;
import com.solarride.solarride.exception.ResourceNotFoundException;
import com.solarride.solarride.repository.UserRepository;
import com.solarride.solarride.security.SolarRideUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    private static final String PENDING_LOGIN_KEY_PREFIX = "pending_login:";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public OtpResponse initiateLogin(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        SolarRideUserDetails ud = (SolarRideUserDetails) auth.getPrincipal();
        if (ud.getStatus() == UserStatus.SUSPENDED) {
            throw new BadCredentialsException("Account suspended");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        // Store user id temporarily for OTP step
        redisTemplate.opsForValue().set(
                PENDING_LOGIN_KEY_PREFIX + user.getPhone(),
                user.getId().toString(),
                300, TimeUnit.SECONDS);

        otpService.sendOtp(user.getPhone());
        log.info("Login OTP sent to user {}", ud.getUserId());
        return new OtpResponse("OTP sent to your registered phone number");
    }

    @Transactional
    public AuthResponse completeLogin(OtpVerifyRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found for phone"));

        String pendingKey = PENDING_LOGIN_KEY_PREFIX + request.phone();
        if (!redisTemplate.hasKey(pendingKey)) {
            throw new BadCredentialsException("No pending login for this phone. Please start login again.");
        }

        if (!otpService.verifyOtp(request.phone(), request.otp())) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        redisTemplate.delete(pendingKey);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshTokens(RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isValid(token)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UUID userId = jwtService.extractUserId(token);
        String storedToken = (String) redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(token)) {
            throw new BadCredentialsException("Refresh token revoked or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
        log.info("User {} logged out", userId);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getStatus());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + user.getId(),
                refreshToken,
                jwtService.getRefreshTtlMs(), TimeUnit.MILLISECONDS);

        log.info("Tokens issued for user {}", user.getId());
        return new AuthResponse(accessToken, refreshToken, user.getRole().name());
    }
}