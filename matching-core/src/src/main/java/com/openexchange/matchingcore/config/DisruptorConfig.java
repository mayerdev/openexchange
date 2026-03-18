package com.openexchange.matchingcore.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.openexchange.matchingcore.disruptor.OrderEvent;
import com.openexchange.matchingcore.disruptor.OrderEventFactory;
import com.openexchange.matchingcore.disruptor.OrderEventHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class DisruptorConfig {

    @Value("${matching.disruptor.ring-buffer-size:1024}")
    private int ringBufferSize;

    @Bean
    public Disruptor<OrderEvent> disruptor(OrderEventHandler orderEventHandler) {
        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                new OrderEventFactory(),
                ringBufferSize,
                Executors.defaultThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith(orderEventHandler);
        disruptor.start();

        return disruptor;
    }

    @Bean
    public RingBuffer<OrderEvent> ringBuffer(Disruptor<OrderEvent> disruptor) {
        return disruptor.getRingBuffer();
    }
}
