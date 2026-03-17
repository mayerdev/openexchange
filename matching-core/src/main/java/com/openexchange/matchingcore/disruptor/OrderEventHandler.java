package com.openexchange.matchingcore.disruptor;

import com.lmax.disruptor.EventHandler;
import com.openexchange.matchingcore.matching.OrderStatusEventListener;
import com.openexchange.matchingcore.matching.TradeEventListener;
import com.openexchange.matchingcore.model.OrderBookSnapshot;
import com.openexchange.matchingcore.model.Trade;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import com.openexchange.matchingcore.repository.OrderBookSnapshotRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final Map<String, SymbolOrderBook> orderBooks = new ConcurrentHashMap<>();
    private final List<TradeEventListener> tradeEventListeners;
    private final List<OrderStatusEventListener> orderStatusEventListeners;
    private final OrderBookSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public OrderEventHandler(List<TradeEventListener> tradeEventListeners,
                             List<OrderStatusEventListener> orderStatusEventListeners,
                             OrderBookSnapshotRepository snapshotRepository,
                             ObjectMapper objectMapper) {
        this.tradeEventListeners = tradeEventListeners;
        this.orderStatusEventListeners = orderStatusEventListeners;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void restoreSnapshots() {
        for (OrderBookSnapshot snapshot : snapshotRepository.findAll()) {
            try {
                List<SymbolOrderBook.RestingOrder> orders = objectMapper.readValue(
                        snapshot.getOrders(),
                        new TypeReference<>() {
                        }
                );

                SymbolOrderBook book = new SymbolOrderBook();
                book.restore(orders);
                orderBooks.put(snapshot.getSymbol(), book);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to restore order book snapshot for symbol: " + snapshot.getSymbol(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void scheduledPersist() {
        persistAll();
    }

    @PreDestroy
    public void onDestroy() {
        persistAll();
    }

    private void persistAll() {
        orderBooks.forEach((symbol, book) -> {
            try {
                List<SymbolOrderBook.RestingOrder> resting = book.getRestingOrders();
                String json = objectMapper.writeValueAsString(resting);
                snapshotRepository.save(new OrderBookSnapshot(symbol, json));
            } catch (Exception e) {
                System.err.println("Failed to persist order book snapshot for symbol: " + symbol + " — " + e.getMessage());
            }
        });
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        var order = event.getOrder();
        if (order == null) return;

        SymbolOrderBook book = orderBooks.computeIfAbsent(order.getSymbol(), s -> new SymbolOrderBook());
        SymbolOrderBook.MatchResult result = book.match(order);

        for (Trade trade : result.trades()) {
            for (TradeEventListener listener : tradeEventListeners) {
                listener.onTrade(trade);
            }
        }

        for (SymbolOrderBook.CancelledEntry cancelled : result.cancelledByStp()) {
            for (OrderStatusEventListener listener : orderStatusEventListeners) {
                listener.onOrderCancelled(cancelled, order.getSymbol());
            }
        }

        if (result.incomingRested()) {
            for (OrderStatusEventListener listener : orderStatusEventListeners) {
                listener.onOrderResting(order, result.incomingRemainingQty());
            }
        }
    }

    public SymbolOrderBook.Snapshot getSnapshot(String symbol) {
        SymbolOrderBook book = orderBooks.get(symbol);
        if (book == null) {
            return new SymbolOrderBook.Snapshot(List.of(), List.of());
        }
        return book.getSnapshot();
    }
}
