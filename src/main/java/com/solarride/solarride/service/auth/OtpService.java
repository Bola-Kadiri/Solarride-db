package com.solarride.solarride.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OtpService {

    private static final int OTP_TTL_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 3;
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String ATTEMPT_KEY_PREFIX = "otp_attempts:";
    private static final String COOLDOWN_KEY_PREFIX = "otp_cooldown:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient termiiWebClient;

    @Value("${termii.api-key:}")
    private String apiKey;

    @Value("${termii.sender-id:SolarRide}")
    private String senderId;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(RedisTemplate<String, Object> redisTemplate,
                      @Qualifier("termiiWebClient") WebClient termiiWebClient) {
        this.redisTemplate = redisTemplate;
        this.termiiWebClient = termiiWebClient;
    }

    public void sendOtp(String phone) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new IllegalStateException("Please wait 60 seconds before requesting another OTP");
        }

        String otp = generateOtp();
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + phone, otp, OTP_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);

        dispatchSms(phone, "Your SolarRide code: " + otp + ". Valid for 5 minutes. Do not share.");
        log.info("OTP dispatched to {}", maskPhone(phone));
    }

    public boolean verifyOtp(String phone, String submittedOtp) {
        String attemptKey = ATTEMPT_KEY_PREFIX + phone;
        Long attempts = redisTemplate.opsForValue().increment(attemptKey);
        if (attempts == null || attempts > MAX_ATTEMPTS) {
            log.warn("OTP max attempts exceeded for {}", maskPhone(phone));
            redisTemplate.delete(OTP_KEY_PREFIX + phone);
            return false;
        }
        redisTemplate.expire(attemptKey, Duration.ofMinutes(10));

        String stored = (String) redisTemplate.opsForValue().get(OTP_KEY_PREFIX + phone);
        if (stored != null && stored.equals(submittedOtp)) {
            redisTemplate.delete(OTP_KEY_PREFIX + phone);
            redisTemplate.delete(attemptKey);
            log.info("OTP verified for {}", maskPhone(phone));
            return true;
        }
        return false;
    }

    private String generateOtp() {
        return String.valueOf(secureRandom.nextInt(900000) + 100000);
    }

    private void dispatchSms(String phone, String message) {
        if (apiKey.isBlank()) {
            log.warn("Termii API key not configured — OTP not sent (dev mode): {}", message);
            return;
        }
        try {
            termiiWebClient.post()
                    .uri("/api/sms/send")
                    .bodyValue(Map.of(
                            "to", phone,
                            "from", senderId,
                            "sms", message,
                            "type", "plain",
                            "channel", "dnd",
                            "api_key", apiKey))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            r -> log.info("Termii SMS sent"),
                            e -> log.error("Termii SMS failed: {}", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to dispatch OTP SMS", e);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}