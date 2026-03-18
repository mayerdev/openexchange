package com.openexchange.matchingcore.charts;

import io.nats.client.Connection;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NatsCandlePublisher {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    public NatsCandlePublisher(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    @Async
    public void publishUpdate(CandleSnapshot candle, String symbol, Timeframe tf) {
        try {
            String subject = "charts.candles." + tf.getLabel() + "." + symbol;
            byte[] payload = objectMapper.writeValueAsBytes(candle);

            connection.publish(subject, payload);
        } catch (Exception e) {
            System.out.println("Failed to publish candle update: " + e.getMessage());
        }
    }
}
