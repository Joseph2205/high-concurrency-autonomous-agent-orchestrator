package com.zaalima.high_concurrency_agent_orchestrator.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store a value without an expiration time.
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Store a value with a TTL.
     */
    public void setWithTtl(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("TTL must be greater than zero");
        }

        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Retrieve a value by key.
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Get the remaining TTL in seconds.
     *
     * Returns:
     * - A positive value when the key has an expiration.
     * - -1 when the key exists without an expiration.
     * - -2 when the key does not exist.
     */
    public long getTtl(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return ttl == null ? -2 : ttl;
    }

    /**
     * Delete a key.
     */
    public boolean delete(String key) {
        Boolean deleted = redisTemplate.delete(key);

        return Boolean.TRUE.equals(deleted);
    }
}