package com.openexchange.matchingcore.chronicle;

import lombok.Getter;
import lombok.Setter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class RestingOrderListHolder extends SelfDescribingMarshallable {

    private List<RestingOrderMarshallable> orders = new ArrayList<>();
    private long lastQueueIndex = 0L;
}
