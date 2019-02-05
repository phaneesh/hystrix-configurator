package com.hystrix.configurator.config;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TimerThreadPoolConfig {

    private int concurrency = Runtime.getRuntime().availableProcessors();

    @Builder
    public TimerThreadPoolConfig(int concurrency) {
        this.concurrency = concurrency;
    }

    public static class TimerThreadPoolConfigBuilder {

        private int concurrency = Runtime.getRuntime().availableProcessors();

    }
}
