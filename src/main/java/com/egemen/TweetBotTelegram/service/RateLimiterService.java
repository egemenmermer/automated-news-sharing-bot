package com.egemen.TweetBotTelegram.service;

public interface RateLimiterService {
    /**
     * Check if the operation can proceed under rate limits
     * @param key Unique identifier for the rate limit bucket
     * @param capacity Maximum number of operations allowed in the interval
     * @param refillIntervalSeconds Time in seconds after which the bucket is refilled
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    void checkRateLimit(String key, int capacity, long refillIntervalSeconds);
}