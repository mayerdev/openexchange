package com.openexchange.matchingcore.matching;

import com.openexchange.matchingcore.model.Order;
import com.openexchange.matchingcore.model.OrderSide;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import io.nats.client.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NatsOrderStatusPublisher implements OrderStatusEventListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public NatsOrderStatusPublisher(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void onOrderResting(Order order, long remainingQty) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", order.getOrderId());
            payload.put("symbol", order.getSymbol());
            payload.put("userId", order.getUserId());
            payload.put("side", order.getSide());
            payload.put("price", order.getPrice());
            payload.put("remainingQty", remainingQty);

            connection.publish("orders.resting." + order.getSymbol(),
                    objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            System.err.println("Failed to publish order resting event: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void onOrderCancelled(SymbolOrderBook.CancelledEntry entry, String symbol) {
        try {
            Long displayPrice = entry.side() == OrderSide.BUY
                    ? (entry.effectivePrice() == Long.MAX_VALUE ? null : entry.effectivePrice())
                    : (entry.effectivePrice() == 0L ? null : entry.effectivePrice());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderId", entry.orderId());
            payload.put("symbol", symbol);
            payload.put("userId", entry.userId());
            payload.put("side", entry.side());
            payload.put("price", displayPrice);
            payload.put("remainingQty", entry.remainingQty());
            payload.put("reason", "SELF_TRADE");

            connection.publish("orders.cancelled." + symbol,
                    objectMapper.writeValueAsBytes(payload));
        } catch (Exception e) {
            System.err.println("Failed to publish order cancelled event: " + e.getMessage());
        }
    }
}
