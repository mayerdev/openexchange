package com.openexchange.matchingcore.matching;

import com.openexchange.matchingcore.model.Trade;
import io.nats.client.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NatsTradePublisher implements TradeEventListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public NatsTradePublisher(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void onTrade(Trade trade) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(trade);

            connection.publish("trades.matched." + trade.getSymbol(), payload);
        } catch (Exception e) {
            System.out.println("Failed to publish trade: " + e.getMessage());
        }
    }
}
