package com.openexchange.matchingcore.orderbook;

import com.openexchange.matchingcore.model.Order;
import com.openexchange.matchingcore.model.OrderSide;
import com.openexchange.matchingcore.model.OrderType;
import com.openexchange.matchingcore.model.Trade;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class SymbolOrderBook {

    private final TreeMap<Long, ArrayDeque<MutableOrderEntry>> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Long, ArrayDeque<MutableOrderEntry>> asks = new TreeMap<>();

    public MatchResult match(Order order) {
        long effectivePrice = effectivePrice(order);
        MutableOrderEntry incoming = new MutableOrderEntry(
                order.getOrderId(), order.getUserId(), effectivePrice, order.getQuantity()
        );

        if (order.getSide() == OrderSide.BUY) {
            addToBook(bids, effectivePrice, incoming);
        } else {
            addToBook(asks, effectivePrice, incoming);
        }

        List<Trade> trades = new ArrayList<>();
        List<CancelledEntry> cancelled = new ArrayList<>();

        while (!bids.isEmpty() && !asks.isEmpty()) {
            long bestBid = bids.firstKey();
            long bestAsk = asks.firstKey();

            if (bestBid < bestAsk) break;

            MutableOrderEntry buy = bids.firstEntry().getValue().peekFirst();
            MutableOrderEntry sell = asks.firstEntry().getValue().peekFirst();

            if (buy == null || sell == null) break;

            if (buy.userId != null && buy.userId.equals(sell.userId)) {
                if (order.getSide() == OrderSide.BUY) {
                    cancelled.add(new CancelledEntry(sell.orderId, sell.userId, OrderSide.SELL, sell.price, sell.remainingQty));
                    removeHead(asks);
                } else {
                    cancelled.add(new CancelledEntry(buy.orderId, buy.userId, OrderSide.BUY, buy.price, buy.remainingQty));
                    removeHead(bids);
                }

                continue;
            }

            long fillQty = Math.min(buy.remainingQty, sell.remainingQty);

            long tradePrice;
            if (buy.price == Long.MAX_VALUE) {
                tradePrice = bestAsk;
            } else if (sell.price == 0L) {
                tradePrice = bestBid;
            } else {
                tradePrice = bestAsk;
            }

            trades.add(Trade.builder()
                    .tradeId(UUID.randomUUID().toString())
                    .symbol(order.getSymbol())
                    .buyOrderId(buy.orderId)
                    .sellOrderId(sell.orderId)
                    .buyUserId(buy.userId)
                    .sellUserId(sell.userId)
                    .quantity(fillQty)
                    .price(tradePrice)
                    .timestamp(System.currentTimeMillis())
                    .build());

            buy.remainingQty -= fillQty;
            sell.remainingQty -= fillQty;

            if (buy.remainingQty == 0) removeHead(bids);
            if (sell.remainingQty == 0) removeHead(asks);
        }

        return new MatchResult(trades, cancelled, incoming.remainingQty > 0, incoming.remainingQty);
    }

    public synchronized Snapshot getSnapshot() {
        List<OrderEntry> bidEntries = new ArrayList<>();
        for (Map.Entry<Long, ArrayDeque<MutableOrderEntry>> level : bids.entrySet()) {
            Long snapshotPrice = level.getKey() == Long.MAX_VALUE ? null : level.getKey();

            for (MutableOrderEntry e : level.getValue()) {
                bidEntries.add(new OrderEntry(e.orderId, snapshotPrice, e.remainingQty));
            }
        }

        List<OrderEntry> askEntries = new ArrayList<>();
        for (Map.Entry<Long, ArrayDeque<MutableOrderEntry>> level : asks.entrySet()) {
            Long snapshotPrice = level.getKey() == 0L ? null : level.getKey();

            for (MutableOrderEntry e : level.getValue()) {
                askEntries.add(new OrderEntry(e.orderId, snapshotPrice, e.remainingQty));
            }
        }

        return new Snapshot(bidEntries, askEntries);
    }

    private static long effectivePrice(Order order) {
        if (order.getOrderType() == OrderType.LIMIT && order.getPrice() != null) {
            return order.getPrice();
        }

        return order.getSide() == OrderSide.BUY ? Long.MAX_VALUE : 0L;
    }

    private static void addToBook(TreeMap<Long, ArrayDeque<MutableOrderEntry>> book, long price, MutableOrderEntry entry) {
        book.computeIfAbsent(price, k -> new ArrayDeque<>()).addLast(entry);
    }

    private static void removeHead(TreeMap<Long, ArrayDeque<MutableOrderEntry>> book) {
        Map.Entry<Long, ArrayDeque<MutableOrderEntry>> level = book.firstEntry();
        if (level == null) return;

        ArrayDeque<MutableOrderEntry> queue = level.getValue();
        queue.pollFirst();

        if (queue.isEmpty()) book.pollFirstEntry();
    }

    private static class MutableOrderEntry {
        final String orderId;
        final String userId;
        final long price;
        long remainingQty;

        MutableOrderEntry(String orderId, String userId, long price, long remainingQty) {
            this.orderId = orderId;
            this.userId = userId;
            this.price = price;
            this.remainingQty = remainingQty;
        }
    }

    public synchronized List<RestingOrder> getRestingOrders() {
        List<RestingOrder> result = new ArrayList<>();

        for (var level : bids.entrySet())
            for (var e : level.getValue())
                result.add(new RestingOrder(e.orderId, e.userId, OrderSide.BUY, e.price, e.remainingQty));

        for (var level : asks.entrySet())
            for (var e : level.getValue())
                result.add(new RestingOrder(e.orderId, e.userId, OrderSide.SELL, e.price, e.remainingQty));

        return result;
    }

    public void restore(List<RestingOrder> orders) {
        for (RestingOrder ro : orders) {
            MutableOrderEntry entry = new MutableOrderEntry(ro.orderId(), ro.userId(), ro.effectivePrice(), ro.remainingQty());
            addToBook(ro.side() == OrderSide.BUY ? bids : asks, ro.effectivePrice(), entry);
        }
    }

    public record OrderEntry(String orderId, Long price, long remainingQty) {
    }

    public record Snapshot(List<OrderEntry> bids, List<OrderEntry> asks) {
    }

    public record RestingOrder(String orderId, String userId, OrderSide side,
                               long effectivePrice, long remainingQty) {
    }

    public record CancelledEntry(String orderId, String userId, OrderSide side,
                                 long effectivePrice, long remainingQty) {
    }

    public record MatchResult(List<Trade> trades, List<CancelledEntry> cancelledByStp,
                              boolean incomingRested, long incomingRemainingQty) {
    }
}
