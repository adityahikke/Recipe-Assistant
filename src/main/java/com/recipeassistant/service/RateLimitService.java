package com.recipeassistant.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Rate limits: 10 requests per minute per IP
    private static final int REQUESTS_PER_MINUTE = 10;
    private static final Duration REFILL_TIME = Duration.ofMinutes(1);
    
    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
        return bucket.tryConsume(1);
    }
    
    public long getAvailableTokens(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
        return bucket.getAvailableTokens();
    }
    
    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, Refill.greedy(REQUESTS_PER_MINUTE, REFILL_TIME));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
}
