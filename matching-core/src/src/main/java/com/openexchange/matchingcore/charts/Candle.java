package com.openexchange.matchingcore.charts;

import lombok.Getter;

public class Candle {
    @Getter
    private long time;
    private long open;
    private long high;
    private long low;
    private long close;
    private long volume;

    public Candle(long time, long open, long high, long low, long close, long volume) {
        this.time = time;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public void update(long price, long qty) {
        if (price > high) high = price;
        if (price < low) low = price;

        close = price;
        volume += qty;
    }

    public CandleSnapshot toSnapshot() {
        return new CandleSnapshot(time, open, high, low, close, volume);
    }
}
