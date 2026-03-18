package com.openexchange.matchingcore.disruptor;

import com.lmax.disruptor.EventHandler;
import com.openexchange.matchingcore.chronicle.*;
import com.openexchange.matchingcore.matching.OrderStatusEventListener;
import com.openexchange.matchingcore.matching.TradeEventListener;
import com.openexchange.matchingcore.model.Trade;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderEventHandler implements EventHandler<OrderEvent> {

    private final Map<String, SymbolOrderBook> orderBooks = new ConcurrentHashMap<>();
    private final List<TradeEventListener> tradeEventListeners;
    private final List<OrderStatusEventListener> orderStatusEventListeners;
    private final OrderBookPersistenceService persistenceService;
    private final ChronicleOrderWriter orderWriter;
    private final ChronicleOrderReader orderReader;
    private final ChronicleMap<String, RestingOrderListHolder> chronicleMap;

    private volatile boolean replayMode = false;

    public OrderEventHandler(List<TradeEventListener> tradeEventListeners,
                             List<OrderStatusEventListener> orderStatusEventListeners,
                             OrderBookPersistenceService persistenceService,
                             ChronicleOrderWriter orderWriter,
                             ChronicleOrderReader orderReader,
                             ChronicleMap<String, RestingOrderListHolder> chronicleMap) {
        this.tradeEventListeners = tradeEventListeners;
        this.orderStatusEventListeners = orderStatusEventListeners;
        this.persistenceService = persistenceService;
        this.orderWriter = orderWriter;
        this.orderReader = orderReader;
        this.chronicleMap = chronicleMap;
    }

    @PostConstruct
    public void restoreSnapshots() {
        if (chronicleMap.isEmpty()) {
            return;
        }

        replayMode = true;

        long minIndex = Long.MAX_VALUE;

        for (Map.Entry<String, RestingOrderListHolder> entry : chronicleMap.entrySet()) {
            String symbol = entry.getKey();
            RestingOrderListHolder holder = entry.getValue();

            SymbolOrderBook book = new SymbolOrderBook();
            book.restore(holder.getOrders().stream()
                    .map(RestingOrderMarshallable::toRestingOrder)
                    .toList());
            orderBooks.put(symbol, book);

            if (holder.getLastQueueIndex() < minIndex) {
                minIndex = holder.getLastQueueIndex();
            }
        }

        if (minIndex > 0 && minIndex != Long.MAX_VALUE) {
            orderReader.replayFrom(minIndex + 1, order -> {
                SymbolOrderBook book = orderBooks.computeIfAbsent(order.getSymbol(), s -> new SymbolOrderBook());
                book.match(order);
            });
        }

        replayMode = false;
    }

    @Scheduled(fixedDelay = 30_000)
    public void scheduledPersist() {
        persistenceService.persistAll(orderBooks);
    }

    @PreDestroy
    public void onDestroy() {
        persistenceService.persistAll(orderBooks);
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        var order = event.getOrder();
        if (order == null) return;

        orderWriter.append(order);

        SymbolOrderBook book = orderBooks.computeIfAbsent(order.getSymbol(), s -> new SymbolOrderBook());
        SymbolOrderBook.MatchResult result = book.match(order);

        if (!replayMode) {
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
    }

    public SymbolOrderBook.Snapshot getSnapshot(String symbol) {
        SymbolOrderBook book = orderBooks.get(symbol);
        if (book == null) {
            return new SymbolOrderBook.Snapshot(List.of(), List.of());
        }

        return book.getSnapshot();
    }
}
