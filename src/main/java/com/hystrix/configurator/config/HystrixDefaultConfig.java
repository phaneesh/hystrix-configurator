package com.hystrix.configurator.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author phaneesh
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HystrixDefaultConfig {

    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    private MetricsConfig metrics = new MetricsConfig();
}
