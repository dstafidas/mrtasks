package com.mrtasks.service.scheduler;

import com.mrtasks.config.RateLimitConfig;
import com.mrtasks.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LogScheduler {

    private final RateLimitConfig rateLimitConfig;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 * * * ?") // Every hour
    public void sendRateLimitLogs() {
        List<String> logs;
        synchronized (rateLimitConfig.getLimitHitLogs()) {
            logs = rateLimitConfig.getLimitHitLogs();
            if (logs.isEmpty()) {
                return; // No logs to send
            }
        }

        String date = LocalDateTime.now().toString();
        emailService.sendLogEmail(logs, date);

        rateLimitConfig.clearLimitHitLogs();
    }
}