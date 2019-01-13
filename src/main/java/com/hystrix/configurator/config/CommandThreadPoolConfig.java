package com.hystrix.configurator.config;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommandThreadPoolConfig {

    private boolean semaphoreIsolation = false;

    private String pool;

    private int concurrency = 8;

    private int maxRequestQueueSize = 128;

    private int dynamicRequestQueueSize = 16;

    private int timeout = 1000;

    @Builder
    public CommandThreadPoolConfig(boolean semaphoreIsolation,
                                   String pool,
                                   int concurrency,
                                   int maxRequestQueueSize,
                                   int dynamicRequestQueueSize,
                                   int timeout) {
        this.semaphoreIsolation = semaphoreIsolation;
        this.pool = pool;
        this.concurrency = concurrency;
        this.maxRequestQueueSize = maxRequestQueueSize;
        this.dynamicRequestQueueSize = dynamicRequestQueueSize;
        this.timeout = timeout;
    }

    //Default values
    public static class CommandThreadPoolConfigBuilder {

        private String pool;

        private boolean semaphoreIsolation = false;

        private int concurrency = Runtime.getRuntime().availableProcessors();

        private int maxRequestQueueSize = Runtime.getRuntime().availableProcessors() * 4;

        private int dynamicRequestQueueSize = Runtime.getRuntime().availableProcessors() * 2;

        private int timeout = 1000;

    }
}
