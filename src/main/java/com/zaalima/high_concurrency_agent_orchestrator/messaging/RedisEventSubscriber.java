 package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisEventSubscriber implements MessageListener {

    private static final Logger logger =
            LoggerFactory.getLogger(RedisEventSubscriber.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel =
                new String(message.getChannel(), StandardCharsets.UTF_8);

        String body =
                new String(message.getBody(), StandardCharsets.UTF_8);

        logger.info(
                "Received Redis Pub/Sub message. Channel: {}, Message: {}",
                channel,
                body
        );
    }
}

