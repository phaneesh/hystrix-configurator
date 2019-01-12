package com.hystrix.configurator.config;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author phaneesh
 */
@Data
@NoArgsConstructor
public class HystrixDefaultConfig {

    private ThreadPoolConfig threadPool = ThreadPoolConfig.builder().build();

    private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.builder().build();

    private MetricsConfig metrics = MetricsConfig.builder().build();

    @Builder
    public HystrixDefaultConfig(ThreadPoolConfig threadPool, CircuitBreakerConfig circuitBreaker, MetricsConfig metrics) {
        this.threadPool = threadPool;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
    }

    public static class HystrixDefaultConfigBuilder {

        private ThreadPoolConfig threadPool = ThreadPoolConfig.builder().build();

        private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.builder().build();

        private MetricsConfig metrics = MetricsConfig.builder().build();

    }
}
