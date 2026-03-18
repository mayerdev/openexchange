package com.openexchange.matchingcore.charts;

import com.openexchange.matchingcore.matching.TradeEventListener;
import com.openexchange.matchingcore.model.Trade;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CandleAggregator implements TradeEventListener {

    private final List<Timeframe> activeTimeframes;
    private final int maxCandlesPerSeries;
    private final NatsCandlePublisher publisher;

    private final Map<String, Map<Timeframe, Candle>> currentCandles = new ConcurrentHashMap<>();
    private final Map<String, Map<Timeframe, LinkedList<CandleSnapshot>>> history = new ConcurrentHashMap<>();

    public CandleAggregator(CandleProperties properties, NatsCandlePublisher publisher) {
        this.publisher = publisher;
        this.maxCandlesPerSeries = properties.getMaxCandlesPerSeries();
        this.activeTimeframes = properties.getTimeframes().stream()
                .map(Timeframe::fromLabel)
                .toList();
    }

    @Override
    public synchronized void onTrade(Trade trade) {
        String symbol = trade.getSymbol();
        long price = trade.getPrice();
        long qty = trade.getQuantity();
        long timestamp = trade.getTimestamp();

        Map<Timeframe, Candle> symbolCandles = currentCandles.computeIfAbsent(symbol, k -> new EnumMap<>(Timeframe.class));

        for (Timeframe tf : activeTimeframes) {
            long bucket = tf.bucketOf(timestamp);
            Candle current = symbolCandles.get(tf);

            if (current == null) {
                current = new Candle(bucket, price, price, price, price, qty);
                symbolCandles.put(tf, current);
            } else if (current.getTime() == bucket) {
                current.update(price, qty);
            } else {
                addToHistory(symbol, tf, current.toSnapshot());
                current = new Candle(bucket, price, price, price, price, qty);
                symbolCandles.put(tf, current);
            }

            publisher.publishUpdate(current.toSnapshot(), symbol, tf);
        }
    }

    public synchronized List<CandleSnapshot> getHistory(String symbol, Timeframe tf) {
        Map<Timeframe, LinkedList<CandleSnapshot>> symbolHistory = history.get(symbol);
        List<CandleSnapshot> result = new ArrayList<>();

        if (symbolHistory != null) {
            LinkedList<CandleSnapshot> snapshots = symbolHistory.get(tf);
            if (snapshots != null) {
                result.addAll(snapshots);
            }
        }

        Map<Timeframe, Candle> symbolCandles = currentCandles.get(symbol);
        if (symbolCandles != null) {
            Candle current = symbolCandles.get(tf);
            if (current != null) {
                result.add(current.toSnapshot());
            }
        }

        return result;
    }

    private void addToHistory(String symbol, Timeframe tf, CandleSnapshot snapshot) {
        Map<Timeframe, LinkedList<CandleSnapshot>> symbolHistory = history.computeIfAbsent(symbol, k -> new EnumMap<>(Timeframe.class));
        LinkedList<CandleSnapshot> list = symbolHistory.computeIfAbsent(tf, k -> new LinkedList<>());
        list.addLast(snapshot);

        if (list.size() > maxCandlesPerSeries) {
            list.removeFirst();
        }
    }
}
