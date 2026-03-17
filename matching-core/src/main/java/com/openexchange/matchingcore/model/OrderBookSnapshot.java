package com.openexchange.matchingcore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_book_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookSnapshot {

    @Id
    private String symbol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String orders;
}
