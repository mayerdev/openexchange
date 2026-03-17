package com.openexchange.matchingcore.repository;

import com.openexchange.matchingcore.model.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, String> {
    Page<Trade> findBySymbolOrderByTimestampDesc(String symbol, Pageable pageable);
}
