package com.openexchange.matchingcore.charts;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class CandleHistoryListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final CandleAggregator aggregator;

    public CandleHistoryListener(Connection connection, ObjectMapper objectMapper, CandleAggregator aggregator) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.aggregator = aggregator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);
        dispatcher.subscribe("charts.history.*.*");
    }

    private void onMessage(Message msg) {
        try {
            if (msg.getReplyTo() == null) return;

            String subject = msg.getSubject();
            String[] parts = subject.split("\\.");

            // parts[0]="charts", parts[1]="history", parts[2]=timeframe, parts[3]=symbol
            Timeframe tf = Timeframe.fromLabel(parts[2]);
            String symbol = parts[3];

            List<CandleSnapshot> candles = aggregator.getHistory(symbol, tf);

            byte[] response = objectMapper.writeValueAsBytes(candles);

            connection.publish(msg.getReplyTo(), response);
        } catch (Exception e) {
            System.out.println("Failed to handle candle history request: " + e.getMessage());
        }
    }
}
