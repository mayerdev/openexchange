package com.openexchange.matchingcore.api;

import com.lmax.disruptor.RingBuffer;
import com.openexchange.matchingcore.disruptor.OrderEvent;
import com.openexchange.matchingcore.model.Order;
import com.openexchange.matchingcore.model.OrderType;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final RingBuffer<OrderEvent> ringBuffer;

    public OrderListener(Connection connection, ObjectMapper objectMapper, RingBuffer<OrderEvent> ringBuffer) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.ringBuffer = ringBuffer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);
        dispatcher.subscribe("orders.new.*", "matching-core");
    }

    private void onMessage(Message msg) {
        try {
            Order order = objectMapper.readValue(msg.getData(), Order.class);
            if (order.getOrderType() == null) order.setOrderType(OrderType.MARKET);

            long sequence = ringBuffer.next();

            try {
                OrderEvent event = ringBuffer.get(sequence);
                event.setOrder(order);
            } finally {
                ringBuffer.publish(sequence);
            }
        } catch (Exception e) {
            System.out.println("Failed to process order: " + e.getMessage());
        }
    }
}
