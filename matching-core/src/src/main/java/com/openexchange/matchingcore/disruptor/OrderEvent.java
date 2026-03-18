package com.openexchange.matchingcore.disruptor;

import com.openexchange.matchingcore.model.Order;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderEvent {
    private Order order;
}
