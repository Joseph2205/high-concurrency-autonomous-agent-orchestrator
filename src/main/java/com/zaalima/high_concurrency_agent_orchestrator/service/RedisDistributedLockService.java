package com.zaalima.high_concurrency_agent_orchestrator.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class RedisDistributedLockService {

    // StringRedisTemplate is used to communicate with Redis.
    // It supports Redis operations involving String keys and values.
    private final StringRedisTemplate redisTemplate;

    // All distributed lock keys will follow this naming convention.
    // Example: orchestrator:lock:agent-1
    private static final String LOCK_PREFIX =
            "orchestrator:lock:";

    // Lua script ensures that lock deletion happens atomically.
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;

    static {

        RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();

        // Lua script executed directly inside Redis.
        //
        // GET KEYS[1] -> Read the current lock owner token.
        // ARGV[1]     -> Token provided by the client requesting release.
        //
        // If both tokens match:
        // DEL KEYS[1] -> Delete the lock.
        //
        // Otherwise:
        // Return 0 without deleting anything.
        RELEASE_LOCK_SCRIPT.setScriptText("""
                if redis.call("get", KEYS[1]) == ARGV[1] then
                    return redis.call("del", KEYS[1])
                else
                    return 0
                end
                """);

        // Redis Lua script returns a Long value.
        // 1 = Lock successfully released.
        // 0 = Lock was not released.
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    // Constructor injection: Spring automatically provides
    // the StringRedisTemplate bean.
    public RedisDistributedLockService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    /**
     * Acquires a distributed lock using SET NX PX.
     *
     * SET  -> Store the lock key and owner token.
     * NX   -> Only create the key if it does not already exist.
     * PX   -> Set an expiration time for the lock.
     *
     * Returns:
     * ownerToken -> Lock successfully acquired.
     * null       -> Lock already exists or acquisition failed.
     */
    public String acquireLock(
            String resource,
            Duration leaseTime) {

        // Validate resource name.
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException(
                    "Resource name cannot be empty"
            );
        }

        // Prevent locks without a valid expiration time.
        if (leaseTime == null || leaseTime.isZero()
                || leaseTime.isNegative()) {
            throw new IllegalArgumentException(
                    "Lease time must be positive"
            );
        }

        // Construct the actual Redis key.
        // Example: orchestrator:lock:payment-service
        String lockKey = LOCK_PREFIX + resource;

        // Generate a unique token for this lock owner.
        // This token identifies which client owns the lock.
        String ownerToken = UUID.randomUUID().toString();

        // SET NX PX operation.
        //
        // setIfAbsent = NX
        // leaseTime   = PX (expiration)
        //
        // Only one application instance can acquire
        // the same resource lock at a time.
        Boolean acquired = redisTemplate
                .opsForValue()
                .setIfAbsent(lockKey, ownerToken, leaseTime);

        // TRUE means this instance acquired the lock.
        if (Boolean.TRUE.equals(acquired)) {
            return ownerToken;
        }

        // Another instance already holds the lock.
        return null;
    }

    /**
     * Releases the lock only if the owner token matches.
     */
    public boolean releaseLock(
            String resource,
            String ownerToken) {

        // Validate resource and owner token.
        if (resource == null || resource.isBlank()
                || ownerToken == null || ownerToken.isBlank()) {
            return false;
        }

        // Reconstruct the Redis lock key.
        String lockKey = LOCK_PREFIX + resource;

        // Execute Lua script atomically inside Redis.
        //
        // KEYS[1] = Redis lock key.
        // ARGV[1] = Owner token.
        //
        // Redis checks ownership and deletes the lock
        // as a single atomic operation.
        Long result = redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                List.of(lockKey),
                ownerToken
        );

        // Return true only when Redis deleted the lock.
        return Long.valueOf(1L).equals(result);
    }
}