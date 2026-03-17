package com.openexchange.matchingcore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Trade {
    @Id
    @EqualsAndHashCode.Include
    private String tradeId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String buyOrderId;

    @Column(nullable = false)
    private String sellOrderId;

    @Column(nullable = false)
    private String buyUserId;

    @Column(nullable = false)
    private String sellUserId;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long timestamp;
}
