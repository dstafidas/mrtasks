package com.mrtasks.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket(long capacity) {
        Refill refill = Refill.intervally(capacity, Duration.ofHours(1));
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    public Bucket getBucket(String key, long capacity) {
        return buckets.computeIfAbsent(key, k -> createBucket(capacity));
    }

    public Bucket getTaskCreationBucket(String username) {
        return getBucket("task-create-" + username, 20);
    }

    public Bucket getClientCreationBucket(String username) {
        return getBucket("client-create-" + username, 20);
    }

    public Bucket getInvoiceDownloadBucket(String username) {
        return getBucket("invoice-download-" + username, 5);
    }

    public Bucket getTaskSearchBucket(String username) {
        return getBucket("task-search-" + username, 50);
    }

    // Client search bucket: 30 per hour
    public Bucket getClientSearchBucket(String username) {
        return getBucket("client-search-" + username, 50);
    }

    // Report generation bucket: 15 per hour
    public Bucket getReportBucket(String username) {
        return getBucket("report-" + username, 32);
    }

    public Bucket getEmailChangeBucket(String username) {
        return getBucket("email-change-" + username, 3);
    }
}