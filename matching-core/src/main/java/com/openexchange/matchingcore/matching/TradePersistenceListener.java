package com.openexchange.matchingcore.matching;

import com.openexchange.matchingcore.model.Trade;
import com.openexchange.matchingcore.repository.TradeRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TradePersistenceListener implements TradeEventListener {

    private final TradeRepository tradeRepository;

    public TradePersistenceListener(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Async
    @Transactional
    @Override
    public void onTrade(Trade trade) {
        tradeRepository.save(trade);
    }
}
