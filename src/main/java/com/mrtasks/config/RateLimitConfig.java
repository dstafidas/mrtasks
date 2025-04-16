package com.mrtasks.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final List<String> limitHitLogs = new ArrayList<>();
    private static final int MAX_LOGS = 1000; // ~300KB max

    private Bucket createBucket(long capacity) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, Duration.ofHours(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getBucket(String key, long capacity) {
        return buckets.computeIfAbsent(key, k -> createBucket(capacity));
    }

    private void addLimitHitLog(String username, String ipAddress, String action) {
        synchronized (limitHitLogs) {
            if (limitHitLogs.size() >= MAX_LOGS) {
                limitHitLogs.removeFirst(); // Drop oldest
            }
            limitHitLogs.add(String.format("%s: User %s, IP %s hit %s limit",
                    java.time.LocalDateTime.now(), username, ipAddress != null ? ipAddress : "unknown", action));
        }
    }

    public boolean canPerformDashboardAction(String username, String ipAddress) {
        Bucket bucket = getBucket("dashboard-" + username, 50); // 50/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "dashboard");
        }
        return allowed;
    }

    public boolean canCreateTask(String username, String ipAddress) {
        Bucket bucket = getBucket("task-create-" + username, 20); // 20/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "task-create");
        }
        return allowed;
    }

    public boolean canCreateClient(String username, String ipAddress) {
        Bucket bucket = getBucket("client-create-" + username, 20); // 20/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "client-create");
        }
        return allowed;
    }

    public boolean canDownloadInvoice(String username, String ipAddress) {
        Bucket bucket = getBucket("invoice-download-" + username, 10); // 10/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "invoice-download");
        }
        return allowed;
    }

    public boolean canSendInvoice(String username, String ipAddress) {
        Bucket bucket = getBucket("invoice-send-" + username, 5); // 5/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "invoice-send");
        }
        return allowed;
    }

    public boolean canSearchTasks(String username, String ipAddress) {
        Bucket bucket = getBucket("task-search-" + username, 50); // 50/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "task-search");
        }
        return allowed;
    }

    public boolean canSearchClients(String username, String ipAddress) {
        Bucket bucket = getBucket("client-search-" + username, 50); // 50/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "client-search");
        }
        return allowed;
    }

    public boolean canGenerateReport(String username, String ipAddress) {
        Bucket bucket = getBucket("report-" + username, 40); // 40/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "report");
        }
        return allowed;
    }

    public boolean canChangeEmail(String username, String ipAddress) {
        Bucket bucket = getBucket("email-change-" + username, 2); // 2/hour
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            addLimitHitLog(username, ipAddress, "email-change");
        }
        return allowed;
    }

    public List<String> getLimitHitLogs() {
        synchronized (limitHitLogs) {
            return new ArrayList<>(limitHitLogs);
        }
    }

    public void clearLimitHitLogs() {
        synchronized (limitHitLogs) {
            limitHitLogs.clear();
        }
    }
}