package com.openexchange.matchingcore.charts;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Setter
@Getter
@ConfigurationProperties("charts")
public class CandleProperties {
    private List<String> timeframes = List.of("1m", "5m", "15m", "1h", "4h", "1d");
    private int maxCandlesPerSeries = 1000;
}
