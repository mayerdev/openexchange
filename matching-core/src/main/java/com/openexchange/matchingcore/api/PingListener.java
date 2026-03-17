package com.openexchange.matchingcore.api;

import tools.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PingListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public PingListener(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);

        dispatcher.subscribe("ping", "matching-core");
    }

    private void onMessage(Message msg) {
        try {
            if(msg.getReplyTo() == null) return;

            connection.publish(msg.getReplyTo(), objectMapper.writeValueAsBytes("pong"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
