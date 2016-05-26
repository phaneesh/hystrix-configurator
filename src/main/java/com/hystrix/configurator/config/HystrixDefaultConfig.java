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

    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    private MetricsConfig metrics = new MetricsConfig();

    @Builder
    public HystrixDefaultConfig(ThreadPoolConfig threadPool, CircuitBreakerConfig circuitBreaker, MetricsConfig metrics) {
        this.threadPool = threadPool;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
    }
}
