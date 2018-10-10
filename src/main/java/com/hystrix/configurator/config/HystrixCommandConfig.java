/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hystrix.configurator.config;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author phaneesh
 */
@Data
@NoArgsConstructor
public class HystrixCommandConfig {

    @NotNull
    private String name;

    @Valid
    private ThreadPoolConfig threadPool = ThreadPoolConfig.builder().build();

    private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.builder().build();

    private MetricsConfig metrics = MetricsConfig.builder().build();

    private boolean fallbackEnabled = true;

    @Builder
    public HystrixCommandConfig(String name,
                                ThreadPoolConfig threadPool,
                                CircuitBreakerConfig circuitBreaker,
                                MetricsConfig metrics,
                                boolean fallbackEnabled) {
        this.name = name;
        this.threadPool = threadPool;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.fallbackEnabled = fallbackEnabled;
    }

    public static class HystrixCommandConfigBuilder {

        private ThreadPoolConfig threadPool = ThreadPoolConfig.builder().build();

        private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.builder().build();

        private MetricsConfig metrics = MetricsConfig.builder().build();

        private boolean fallbackEnabled = true;

    }
}
