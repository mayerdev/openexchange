package com.openexchange.matchingcore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    private String tradeId;
    private String symbol;
    private String buyOrderId;
    private String sellOrderId;
    private String buyUserId;
    private String sellUserId;
    private long quantity;
    private long price;
    private long timestamp;
}
