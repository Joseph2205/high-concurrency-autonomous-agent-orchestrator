package com.zaalima.high_concurrency_agent_orchestrator.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    // StringRedisTemplate provides methods to communicate with Redis.
    // It handles Redis operations using String keys and String values.
    private final StringRedisTemplate redisTemplate;

    // Constructor injection.
    // Spring automatically provides the StringRedisTemplate bean.
    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store a value without an expiration time.
     *
     * Redis command equivalent:
     * SET key value
     *
     * The value remains in Redis until manually deleted
     * or removed through another Redis operation.
     */
    public void set(String key, String value) {

        // opsForValue() provides Redis String value operations.
        // set() stores the key-value pair in Redis.
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Store a value with a TTL (Time To Live).
     *
     * Redis command equivalent:
     * SET key value EX seconds
     *
     * The key automatically expires after the specified duration.
     */
    public void setWithTtl(String key, String value, Duration ttl) {

        // Validate the expiration duration.
        // A TTL must be provided and must be positive.
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "TTL must be greater than zero");
        }

        // Store the key-value pair with an expiration time.
        // Redis automatically deletes the key after the TTL expires.
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * Retrieve a value by key.
     *
     * Redis command equivalent:
     * GET key
     *
     * Returns:
     * The stored value if the key exists.
     * null if the key does not exist.
     */
    public String get(String key) {

        // Retrieve the value associated with the given Redis key.
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Get the remaining TTL in seconds.
     *
     * Redis command equivalent:
     * TTL key
     *
     * Returns:
     * Positive value -> Remaining expiration time.
     * -1              -> Key exists without expiration.
     * -2              -> Key does not exist.
     */
    public long getTtl(String key) {

        // getExpire() retrieves the remaining expiration time.
        // TimeUnit.SECONDS specifies that the result should be in seconds.
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        // If Redis returns null, use -2 as the fallback.
        return ttl == null ? -2 : ttl;
    }

    /**
     * Delete a key from Redis.
     *
     * Redis command equivalent:
     * DEL key
     *
     * Returns:
     * true  -> Key successfully deleted.
     * false -> Key did not exist or deletion was unsuccessful.
     */
    public boolean delete(String key) {

        // Delete the specified Redis key.
        Boolean deleted = redisTemplate.delete(key);

        // Convert the Boolean result into a primitive boolean.
        // Handles a possible null response safely.
        return Boolean.TRUE.equals(deleted);
    }
}