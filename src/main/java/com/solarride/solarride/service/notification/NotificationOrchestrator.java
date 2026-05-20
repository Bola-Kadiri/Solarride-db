package com.solarride.solarride.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Fires both FCM push and Termii SMS for every critical platform event.
 * Stub — full implementation wired in Step 31.
 */
@Slf4j
@Service
public class NotificationOrchestrator {

    @Async
    public void notify(UUID userId, String eventType, String message) {
        log.info("Notification [{}] to user {}: {}", eventType, userId, message);
        // TODO Step 31: wire PushNotificationService (FCM) + SmsService (Termii)
    }

    @Async
    public void notifyByPhone(String phone, String message) {
        log.info("SMS notification to {}: {}", maskPhone(phone), message);
        // TODO Step 31: wire SmsService
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
