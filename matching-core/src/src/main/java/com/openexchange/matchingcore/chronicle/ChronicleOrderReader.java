package com.openexchange.matchingcore.chronicle;

import com.openexchange.matchingcore.model.Order;
import com.openexchange.matchingcore.model.OrderSide;
import com.openexchange.matchingcore.model.OrderType;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class ChronicleOrderReader {

    private final ChronicleQueue orderQueue;

    public ChronicleOrderReader(ChronicleQueue orderQueue) {
        this.orderQueue = orderQueue;
    }

    public void replayFrom(long fromIndex, Consumer<Order> handler) {
        try (ExcerptTailer tailer = orderQueue.createTailer("recovery")) {
            if (!tailer.moveToIndex(fromIndex)) {
                return;
            }

            while (tailer.readDocument(r -> {
                Order order = new Order();
                order.setOrderId(r.read("orderId").text());
                order.setSymbol(r.read("symbol").text());
                order.setUserId(r.read("userId").text());
                order.setSide(OrderSide.valueOf(r.read("side").text()));
                OrderType orderType = OrderType.valueOf(r.read("orderType").text());
                order.setOrderType(orderType);
                long price = r.read("price").int64();
                order.setPrice(price == 0L && orderType == OrderType.MARKET ? null : price);
                order.setQuantity(r.read("quantity").int64());
                order.setTimestamp(r.read("timestamp").int64());

                handler.accept(order);
            })) {
            }
        }
    }
}
