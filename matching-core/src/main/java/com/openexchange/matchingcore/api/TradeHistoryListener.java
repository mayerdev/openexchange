package com.openexchange.matchingcore.api;

import com.openexchange.matchingcore.model.Trade;
import com.openexchange.matchingcore.repository.TradeRepository;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class TradeHistoryListener {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final TradeRepository tradeRepository;

    public TradeHistoryListener(Connection connection, ObjectMapper objectMapper, TradeRepository tradeRepository) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.tradeRepository = tradeRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Dispatcher dispatcher = connection.createDispatcher(this::onMessage);
        dispatcher.subscribe("trades.history.*", "matching-core");
    }

    private void onMessage(Message msg) {
        try {
            if (msg.getReplyTo() == null) return;

            String subject = msg.getSubject();
            String symbol = subject.substring(subject.lastIndexOf('.') + 1);

            int page = 0;
            int size = 50;
            byte[] data = msg.getData();
            if (data != null && data.length > 0) {
                JsonNode node = objectMapper.readTree(data);
                if (node.has("page")) page = node.get("page").intValue();
                if (node.has("size")) size = node.get("size").intValue();
            }

            Page<Trade> result = tradeRepository.findBySymbolOrderByTimestampDesc(
                    symbol, PageRequest.of(page, size)
            );

            List<Trade> trades = result.getContent();

            byte[] response = objectMapper.writeValueAsBytes(Map.of(
                    "symbol", symbol,
                    "page", page,
                    "size", size,
                    "trades", trades
            ));

            connection.publish(msg.getReplyTo(), response);
        } catch (Exception e) {
            System.out.println("Failed to handle trade history request: " + e.getMessage());
        }
    }
}
