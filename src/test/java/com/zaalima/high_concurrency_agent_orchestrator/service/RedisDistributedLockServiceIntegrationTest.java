package com.zaalima.high_concurrency_agent_orchestrator.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RedisDistributedLockServiceIntegrationTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private LettuceConnectionFactory factory1;
    private LettuceConnectionFactory factory2;

    private RedisDistributedLockService instance1;
    private RedisDistributedLockService instance2;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @BeforeEach
    void setUp() {

        // First independent Redis connection
        factory1 = createConnectionFactory();

        // Second independent Redis connection
        factory2 = createConnectionFactory();

        StringRedisTemplate template1 = createTemplate(factory1);
        StringRedisTemplate template2 = createTemplate(factory2);

        // Simulate two application instances
        instance1 = new RedisDistributedLockService(template1);
        instance2 = new RedisDistributedLockService(template2);
    }

    @AfterEach
    void tearDown() {
        factory1.destroy();
        factory2.destroy();
    }

    private LettuceConnectionFactory createConnectionFactory() {

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(
                        REDIS.getHost(),
                        REDIS.getMappedPort(6379)
                );

        factory.afterPropertiesSet();

        return factory;
    }

    private StringRedisTemplate createTemplate(
            LettuceConnectionFactory factory) {

        StringRedisTemplate template = new StringRedisTemplate();

        template.setConnectionFactory(factory);
        template.afterPropertiesSet();

        return template;
    }

    @Test
    void shouldAllowOnlyOneInstanceToAcquireLock() {

        String resource = "payment";

        // Instance 1 acquires the lock
        String token1 = instance1.acquireLock(
                resource,
                Duration.ofSeconds(10)
        );

        assertNotNull(token1);

        // Instance 2 attempts to acquire the same lock
        String token2 = instance2.acquireLock(
                resource,
                Duration.ofSeconds(10)
        );

        // Instance 2 must fail
        assertNull(token2);

        // Instance 1 releases its lock
        assertTrue(instance1.releaseLock(resource, token1));

        // Instance 2 can now acquire the lock
        String token3 = instance2.acquireLock(
                resource,
                Duration.ofSeconds(10)
        );

        assertNotNull(token3);

        assertTrue(instance2.releaseLock(resource, token3));
    }

    @Test
    void shouldNotAllowWrongOwnerToReleaseLock() {

        String resource = "transaction";

        String ownerToken = instance1.acquireLock(
                resource,
                Duration.ofSeconds(10)
        );

        assertNotNull(ownerToken);

        // Instance 2 tries to release Instance 1's lock
        boolean released = instance2.releaseLock(
                resource,
                "wrong-owner-token"
        );

        assertFalse(released);

        // The original lock must still exist
        assertNull(
                instance2.acquireLock(
                        resource,
                        Duration.ofSeconds(10)
                )
        );

        // Correct owner releases the lock
        assertTrue(instance1.releaseLock(resource, ownerToken));
    }

    @Test
    void shouldAutomaticallyReleaseLockAfterExpiry()
            throws InterruptedException {

        String resource = "order-processing";

        // Instance 1 acquires a lock for 800 milliseconds
        String oldToken = instance1.acquireLock(
                resource,
                Duration.ofMillis(800)
        );

        assertNotNull(oldToken);

        // Instance 2 cannot acquire it yet
        assertNull(
                instance2.acquireLock(
                        resource,
                        Duration.ofSeconds(10)
                )
        );

        // Wait until the first lock expires
        String newToken = null;

        long deadline = System.currentTimeMillis() + 5000;

        while (System.currentTimeMillis() < deadline) {

            newToken = instance2.acquireLock(
                    resource,
                    Duration.ofSeconds(10)
            );

            if (newToken != null) {
                break;
            }

            Thread.sleep(50);
        }

        // Instance 2 should acquire after expiry
        assertNotNull(newToken);

        // Old owner must not release the new owner's lock
        assertFalse(instance1.releaseLock(resource, oldToken));

        // New owner can release its own lock
        assertTrue(instance2.releaseLock(resource, newToken));
    }
}