package com.openexchange.matchingcore;

import com.openexchange.matchingcore.chronicle.ChronicleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ChronicleProperties.class)
public class MatchingCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchingCoreApplication.class, args);
    }
}
