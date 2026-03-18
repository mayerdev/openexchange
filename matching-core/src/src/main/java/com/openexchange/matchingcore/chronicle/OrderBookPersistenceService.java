package com.openexchange.matchingcore.chronicle;

import com.openexchange.matchingcore.orderbook.SymbolOrderBook;
import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderBookPersistenceService {

    private final ChronicleMap<String, RestingOrderListHolder> orderBookMap;
    private final ChronicleOrderWriter chronicleOrderWriter;

    public OrderBookPersistenceService(ChronicleMap<String, RestingOrderListHolder> orderBookMap,
                                       ChronicleOrderWriter chronicleOrderWriter) {
        this.orderBookMap = orderBookMap;
        this.chronicleOrderWriter = chronicleOrderWriter;
    }

    public void persistAll(Map<String, SymbolOrderBook> orderBooks) {
        long idx = Math.max(chronicleOrderWriter.lastIndex(), 0L);

        orderBooks.forEach((symbol, book) -> {
            List<RestingOrderMarshallable> marshalled = book.getRestingOrders().stream()
                    .map(RestingOrderMarshallable::from)
                    .collect(Collectors.toList());

            RestingOrderListHolder holder = new RestingOrderListHolder();
            holder.setLastQueueIndex(idx);
            holder.setOrders(marshalled);

            orderBookMap.put(symbol, holder);
        });
    }
}
