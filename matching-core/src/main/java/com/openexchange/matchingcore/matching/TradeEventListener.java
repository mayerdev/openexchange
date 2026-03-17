package com.openexchange.matchingcore.matching;

import com.openexchange.matchingcore.model.Trade;

public interface TradeEventListener {
    void onTrade(Trade trade);
}
