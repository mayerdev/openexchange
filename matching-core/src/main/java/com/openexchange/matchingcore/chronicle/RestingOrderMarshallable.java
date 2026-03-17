package com.openexchange.matchingcore.chronicle;

import com.openexchange.matchingcore.model.OrderSide;
import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import lombok.Getter;
import lombok.Setter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

@Setter
@Getter
public class RestingOrderMarshallable extends SelfDescribingMarshallable {

    private String orderId;
    private String userId;
    private String side;
    private long effectivePrice;
    private long remainingQty;

    public RestingOrderMarshallable() {}

    public static RestingOrderMarshallable from(SymbolOrderBook.RestingOrder ro) {
        RestingOrderMarshallable m = new RestingOrderMarshallable();
        m.orderId = ro.orderId();
        m.userId = ro.userId();
        m.side = ro.side().name();
        m.effectivePrice = ro.effectivePrice();
        m.remainingQty = ro.remainingQty();
        return m;
    }

    public SymbolOrderBook.RestingOrder toRestingOrder() {
        return new SymbolOrderBook.RestingOrder(orderId, userId, OrderSide.valueOf(side), effectivePrice, remainingQty);
    }
}
