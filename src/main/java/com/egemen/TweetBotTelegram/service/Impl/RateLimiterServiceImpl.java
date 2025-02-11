package com.egemen.TweetBotTelegram.service.Impl;

import com.egemen.TweetBotTelegram.exception.RateLimitExceededException;
import com.egemen.TweetBotTelegram.service.RateLimiterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterServiceImpl implements RateLimiterService {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    private static class TokenBucket {
        private int tokens;
        private final int capacity;
        private LocalDateTime lastRefill;
        private final long refillIntervalSeconds;
        
        public TokenBucket(int capacity, long refillIntervalSeconds) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = LocalDateTime.now();
            this.refillIntervalSeconds = refillIntervalSeconds;
        }
        
        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
        
        private void refill() {
            LocalDateTime now = LocalDateTime.now();
            long secondsElapsed = java.time.Duration.between(lastRefill, now).getSeconds();
            if (secondsElapsed >= refillIntervalSeconds) {
                tokens = capacity;
                lastRefill = now;
            }
        }
    }
    
    @Override
    public void checkRateLimit(String key, int capacity, long refillIntervalSeconds) {
        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(capacity, refillIntervalSeconds));
                
        if (!bucket.tryConsume()) {
            throw new RateLimitExceededException("Rate limit exceeded for: " + key);
        }
    }
}