package com.openexchange.matchingcore.repository;

import com.openexchange.matchingcore.model.OrderBookSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderBookSnapshotRepository extends JpaRepository<OrderBookSnapshot, String> {}
