package com.openexchange.matchingcore.config;

import tools.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NatsConfig {

    @Value("${nats.url}")
    private String natsUrl;

    @Bean
    public Connection connection() throws Exception {
        Options options = new Options.Builder().server(natsUrl).build();

        return Nats.connect(options);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
