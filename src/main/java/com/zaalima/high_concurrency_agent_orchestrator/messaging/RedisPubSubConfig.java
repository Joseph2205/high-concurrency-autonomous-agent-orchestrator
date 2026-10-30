package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    /**
     * Defines the Redis Pub/Sub channel used for broadcasting
     * cluster node events between orchestrator instances.
     */
    public static final String NODE_EVENTS_CHANNEL =
            "orchestrator:node-events";

    public static final String NODE_EVENTS_CHANNEL =
            "orchestrator:node-events";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisEventSubscriber subscriber) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                subscriber,
                new ChannelTopic(NODE_EVENTS_CHANNEL)
        );

        return container;
    }
}

