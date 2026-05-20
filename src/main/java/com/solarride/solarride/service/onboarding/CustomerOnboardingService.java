package com.solarride.solarride.service.onboarding;

import com.solarride.solarride.domain.user.Role;
import com.solarride.solarride.domain.user.User;
import com.solarride.solarride.domain.user.UserStatus;
import com.solarride.solarride.dto.request.CustomerRegisterRequest;
import com.solarride.solarride.dto.request.OtpVerifyRequest;
import com.solarride.solarride.dto.response.UserResponse;
import com.solarride.solarride.exception.DuplicateResourceException;
import com.solarride.solarride.repository.UserRepository;
import com.solarride.solarride.service.auth.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOnboardingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional
    public UserResponse register(CustomerRegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone number already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);
        log.info("Customer registered: {}", user.getId());

        otpService.sendOtp(user.getPhone());
        return toResponse(user);
    }

    @Transactional
    public UserResponse verifyPhone(OtpVerifyRequest request) {
        User user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new IllegalArgumentException("No user with that phone"));

        if (!otpService.verifyOtp(request.phone(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        user.setPhoneVerified(true);
        userRepository.save(user);
        log.info("Phone verified for user {}", user.getId());
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.isPhoneVerified());
    }
}