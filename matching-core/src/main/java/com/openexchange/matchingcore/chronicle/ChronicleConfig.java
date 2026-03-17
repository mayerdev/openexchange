package com.openexchange.matchingcore.chronicle;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;

@Configuration
public class ChronicleConfig {

    private static final Logger log = LoggerFactory.getLogger(ChronicleConfig.class);

    private ChronicleMap<String, RestingOrderListHolder> orderBookMap;
    private ChronicleQueue orderQueue;

    @Bean
    public ChronicleMap<String, RestingOrderListHolder> orderBookMap(ChronicleProperties props) throws IOException {
        File dataDir = new File(props.getDataDir());
        dataDir.mkdirs();

        File mapFile = new File(dataDir, "order-book-snapshots.dat");
        orderBookMap = openOrRecreateMap(mapFile, props);

        return orderBookMap;
    }

    private ChronicleMap<String, RestingOrderListHolder> openOrRecreateMap(File mapFile, ChronicleProperties props) throws IOException {
        try {
            return buildMap(mapFile, props);
        } catch (Exception e) {
            log.warn("Chronicle Map file appears corrupted ({}), deleting and recreating", e.getMessage());
            mapFile.delete();

            return buildMap(mapFile, props);
        }
    }

    private ChronicleMap<String, RestingOrderListHolder> buildMap(File mapFile, ChronicleProperties props) throws IOException {
        return ChronicleMap
                .of(String.class, RestingOrderListHolder.class)
                .name("order-book-snapshots")
                .entries(props.getMapMaxEntries())
                .averageKeySize(props.getMapAverageKeySize())
                .averageValueSize(props.getMapAverageValueSize())
                .createPersistedTo(mapFile);
    }

    @Bean
    public ChronicleQueue orderQueue(ChronicleProperties props) {
        File queueDir = new File(props.getDataDir(), "order-queue");
        queueDir.mkdirs();

        orderQueue = SingleChronicleQueueBuilder.binary(queueDir).build();

        return orderQueue;
    }

    @PreDestroy
    public void close() {
        if (orderBookMap != null && !orderBookMap.isClosed()) {
            orderBookMap.close();
        }

        if (orderQueue != null && !orderQueue.isClosed()) {
            orderQueue.close();
        }
    }
}
