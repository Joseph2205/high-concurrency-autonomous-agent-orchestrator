package com.zaalima.high_concurrency_agent_orchestrator.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisDistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX =
            "orchestrator:lock:";

    // Atomic Lua script for safe lock release
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;

    static {

        RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();

        RELEASE_LOCK_SCRIPT.setScriptText("""
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
                """);

        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    public RedisDistributedLockService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    /**
     * Acquires a distributed lock using SET NX PX.
     */
    public String acquireLock(
            String resource,
            Duration leaseTime) {

        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException(
                    "Resource name cannot be empty"
            );
        }

        if (leaseTime == null || leaseTime.isZero()
                || leaseTime.isNegative()) {
            throw new IllegalArgumentException(
                    "Lease time must be positive"
            );
        }

        String lockKey = LOCK_PREFIX + resource;

        String ownerToken = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate
                .opsForValue()
                .setIfAbsent(lockKey, ownerToken, leaseTime);

        if (Boolean.TRUE.equals(acquired)) {
            return ownerToken;
        }

        return null;
    }

    /**
     * Releases the lock only if the owner token matches.
     */
    public boolean releaseLock(
            String resource,
            String ownerToken) {

        if (resource == null || resource.isBlank()
                || ownerToken == null || ownerToken.isBlank()) {
            return false;
        }

        String lockKey = LOCK_PREFIX + resource;

        Long result = redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                List.of(lockKey),
                ownerToken
        );

        return Long.valueOf(1L).equals(result);
    }
}