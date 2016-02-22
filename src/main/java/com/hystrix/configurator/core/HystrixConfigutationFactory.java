package com.hystrix.configurator.core;

import com.hystrix.configurator.config.*;
import com.netflix.hystrix.*;

import java.security.InvalidParameterException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author phaneesh
 */
public class HystrixConfigutationFactory {

    private final HystrixConfig config;

    private HystrixDefaultConfig defaultConfig;

    private Map<String, HystrixCommandConfig> commandConfigMap;

    private Map<String, HystrixCommand.Setter> hystrixConfigCache;

    private static HystrixConfigutationFactory factory;

    private HystrixConfigutationFactory(final HystrixConfig config) {
        this.config = config;
    }

    public static void init(final HystrixConfig config) {
        if(factory == null) {
            factory = new HystrixConfigutationFactory(config);
        }
        factory.setup();
    }

    private void setup() {
        if(config != null) {
            defaultConfig = config.getDefaultConfig();
            if(defaultConfig == null) {
                defaultConfig = new HystrixDefaultConfig();
            }
            if(defaultConfig.getThreadPool() == null)
                defaultConfig.setThreadPool(new ThreadPoolConfig());
            if(defaultConfig.getCircuitBreaker() == null)
                defaultConfig.setCircuitBreaker(new CircuitBreakerConfig());
            if(defaultConfig.getMetrics() == null)
                defaultConfig.setMetrics(new MetricsConfig());
            commandConfigMap = config.getCommands().stream().collect(Collectors.toMap(HystrixCommandConfig::getName, (c) -> c));
            commandConfigMap.forEach( (k,v) -> {
                if(v.getCircuitBreaker() == null)
                    v.setCircuitBreaker(defaultConfig.getCircuitBreaker());
                if(v.getThreadPool() == null)
                    v.setThreadPool(defaultConfig.getThreadPool());
                if(v.getMetrics() == null) {
                    v.setMetrics(defaultConfig.getMetrics());
                }
            });
            hystrixConfigCache = commandConfigMap.values().stream().collect(Collectors.toMap(HystrixCommandConfig::getName, (c) ->
                HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(c.getName()))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(c.getName()))
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(c.getName()))
                    .andCommandPropertiesDefaults(
                            HystrixCommandProperties.Setter()
                                .withExecutionIsolationStrategy( c.getThreadPool().isSemaphoreIsolation() ? HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE: HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                            .withExecutionIsolationSemaphoreMaxConcurrentRequests(c.getThreadPool().getConcurrency())
                            .withFallbackIsolationSemaphoreMaxConcurrentRequests(c.getThreadPool().getConcurrency())
                            .withFallbackEnabled(c.isFallbackEnabled())
                            .withCircuitBreakerErrorThresholdPercentage(c.getCircuitBreaker().getErrorThreshold())
                            .withCircuitBreakerRequestVolumeThreshold(c.getCircuitBreaker().getAcceptableFailuresInWindow())
                            .withCircuitBreakerSleepWindowInMilliseconds(c.getCircuitBreaker().getWaitTimeBeforeRetry())
                            .withExecutionTimeoutInMilliseconds(c.getThreadPool().getTimeout())
                            .withMetricsHealthSnapshotIntervalInMilliseconds(c.getMetrics().getHealthCheckInterval())
                            .withMetricsRollingPercentileBucketSize(c.getMetrics().getPercentileBucketSize())
                            .withMetricsRollingPercentileWindowInMilliseconds(c.getMetrics().getPercentileTimeInMillis())
                    )
                    .andThreadPoolPropertiesDefaults(
                            HystrixThreadPoolProperties.Setter()
                                .withCoreSize(c.getThreadPool().getConcurrency())
                                .withMaxQueueSize(c.getThreadPool().getMaxRequestQueueSize())
                                .withQueueSizeRejectionThreshold(c.getThreadPool().getDynamicRequestQueueSize())
                                .withMetricsRollingStatisticalWindowBuckets(c.getMetrics().getNumBucketSize())
                                .withMetricsRollingStatisticalWindowInMilliseconds(c.getMetrics().getStatsTimeInMillis())
                    )
            ));
        }
    }

    public static HystrixCommand.Setter getCommandConfiguration(final String key) {
        if(factory == null) throw new IllegalStateException("Factory not initialized");
        if(!factory.hystrixConfigCache.containsKey(key)) throw new InvalidParameterException("Invalid command key");
        return factory.hystrixConfigCache.get(key);
    }
}
