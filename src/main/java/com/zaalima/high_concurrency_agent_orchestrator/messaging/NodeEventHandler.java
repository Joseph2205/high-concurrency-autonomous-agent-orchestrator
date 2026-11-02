package com.zaalima.high_concurrency_agent_orchestrator.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NodeEventHandler {


    private static final Logger logger =
            LoggerFactory.getLogger(NodeEventHandler.class);

    public void handle(NodeEvent event) {

        // Validate incoming event
        if (event == null || event.eventType() == null) {
            logger.warn("Received invalid node event");
            return;
        }

        switch (event.eventType()) {

            case NODE_JOINED -> handleNodeJoined(event);

            case NODE_LEFT -> handleNodeLeft(event);

            case NODE_HEARTBEAT -> handleHeartbeat(event);

            case NODE_UNHEALTHY -> handleNodeUnhealthy(event);
        }
    }

    private void handleNodeJoined(NodeEvent event) {

        logger.info(
                "Node joined cluster: {}",
                event.nodeId()
        );
    }

    private void handleNodeLeft(NodeEvent event) {

        logger.info(
                "Node left cluster: {}",
                event.nodeId()
        );
    }

    private void handleHeartbeat(NodeEvent event) {

        logger.debug(
                "Heartbeat received from node: {}",
                event.nodeId()
        );
    }

    private void handleNodeUnhealthy(NodeEvent event) {

        logger.warn(
                "Node reported unhealthy: {}",
                event.nodeId()
        );
    }


}
