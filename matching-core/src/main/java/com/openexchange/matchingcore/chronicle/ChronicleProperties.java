package com.openexchange.matchingcore.chronicle;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("chronicle")
public class ChronicleProperties {
    private String dataDir = "./data";
    private long mapMaxEntries = 1000;
    private int mapAverageKeySize = 10;
    private int mapAverageValueSize = 4096;
}
