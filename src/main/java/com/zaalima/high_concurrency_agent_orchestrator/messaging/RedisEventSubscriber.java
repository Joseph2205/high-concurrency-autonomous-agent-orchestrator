package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

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

    private final JsonMapper jsonMapper;

    private final NodeEventHandler nodeEventHandler;

    public RedisEventSubscriber(
            JsonMapper jsonMapper,
            NodeEventHandler nodeEventHandler) {

        this.jsonMapper = jsonMapper;
        this.nodeEventHandler = nodeEventHandler;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String channel = new String(
                message.getChannel(),
                StandardCharsets.UTF_8
        );

        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8
        );

        try {

            // Convert incoming JSON into NodeEvent
            NodeEvent event = jsonMapper.readValue(
                    body,
                    NodeEvent.class
            );

            logger.info(
                    "Received Redis Pub/Sub message. Channel: {}, Event: {}",
                    channel,
                    event != null ? event.eventType() : "null"
            );

            // Forward event to NodeEventHandler
            nodeEventHandler.handle(event);

        } catch (JacksonException exception) {

            logger.error(
                    "Failed to deserialize Redis message from channel: {}",
                    channel,
                    exception
            );
        }
    }

}
