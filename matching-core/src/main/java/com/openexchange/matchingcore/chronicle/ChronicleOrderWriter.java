package com.openexchange.matchingcore.chronicle;

import com.openexchange.matchingcore.model.Order;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.springframework.stereotype.Component;

@Component
public class ChronicleOrderWriter {

    private final ExcerptAppender appender;

    public ChronicleOrderWriter(ChronicleQueue orderQueue) {
        this.appender = orderQueue.createAppender();
    }

    public void append(Order order) {
        appender.writeDocument(w -> {
            w.write("orderId").text(order.getOrderId());
            w.write("symbol").text(order.getSymbol());
            w.write("userId").text(order.getUserId());
            w.write("side").text(order.getSide().name());
            w.write("orderType").text(order.getOrderType().name());
            w.write("price").int64(order.getPrice() != null ? order.getPrice() : 0L);
            w.write("quantity").int64(order.getQuantity());
            w.write("timestamp").int64(order.getTimestamp());
        });
    }

    public long lastIndex() {
        try {
            return appender.lastIndexAppended();
        } catch (IllegalStateException e) {
            return -1L;
        }
    }
}
