package com.openexchange.matchingcore.matching;

import com.openexchange.matchingcore.model.Order;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;

public interface OrderStatusEventListener {
    void onOrderResting(Order order, long remainingQty);
    void onOrderCancelled(SymbolOrderBook.CancelledEntry entry, String symbol);
}
