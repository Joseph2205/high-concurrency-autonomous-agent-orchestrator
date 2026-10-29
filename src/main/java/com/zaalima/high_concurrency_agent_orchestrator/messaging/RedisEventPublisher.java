
package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisEventPublisher {

    private final StringRedisTemplate redisTemplate;

    public RedisEventPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(String channel, String message) {
        Long subscriberCount =
                redisTemplate.convertAndSend(channel, message);

        System.out.println(
                "Published message to channel: " + channel
                        + ", subscribers: " + subscriberCount
        );
    }
}

