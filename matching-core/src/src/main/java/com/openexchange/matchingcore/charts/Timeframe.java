package com.openexchange.matchingcore.charts;

import lombok.Getter;

@Getter
public enum Timeframe {
    ONE_MIN("1m", 60),
    FIVE_MIN("5m", 300),
    FIFTEEN_MIN("15m", 900),
    ONE_HOUR("1h", 3600),
    FOUR_HOUR("4h", 14400),
    ONE_DAY("1d", 86400);

    private final String label;
    private final long durationSeconds;

    Timeframe(String label, long durationSeconds) {
        this.label = label;
        this.durationSeconds = durationSeconds;
    }

    public long bucketOf(long timestampMillis) {
        return (timestampMillis / 1000 / durationSeconds) * durationSeconds;
    }

    public static Timeframe fromLabel(String label) {
        for (Timeframe tf : values()) {
            if (tf.label.equals(label)) return tf;
        }

        throw new IllegalArgumentException("Unknown timeframe: " + label);
    }
}
