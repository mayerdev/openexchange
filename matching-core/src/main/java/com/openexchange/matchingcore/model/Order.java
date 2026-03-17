package com.openexchange.matchingcore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderId;
    private String symbol;
    private String userId;
    private OrderSide side;
    private OrderType orderType;
    private Long price;
    private long quantity;
    private long timestamp;
}
