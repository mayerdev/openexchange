package com.openexchange.matchingcore.api;

import com.openexchange.matchingcore.disruptor.OrderEventHandler;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class OrderBookStateListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final OrderEventHandler orderEventHandler;

    public OrderBookStateListener(Connection connection, ObjectMapper objectMapper, OrderEventHandler orderEventHandler) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.orderEventHandler = orderEventHandler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);
        dispatcher.subscribe("orderbook.state.*", "matching-core");
    }

    private void onMessage(Message msg) {
        try {
            if (msg.getReplyTo() == null) return;

            String subject = msg.getSubject();
            String symbol = subject.substring(subject.lastIndexOf('.') + 1);

            SymbolOrderBook.Snapshot snapshot = orderEventHandler.getSnapshot(symbol);

            byte[] response = objectMapper.writeValueAsBytes(Map.of(
                    "symbol", symbol,
                    "bids", snapshot.bids(),
                    "asks", snapshot.asks()
            ));

            connection.publish(msg.getReplyTo(), response);
        } catch (Exception e) {
            System.out.println("Failed to handle orderbook state request: " + e.getMessage());
        }
    }
}
