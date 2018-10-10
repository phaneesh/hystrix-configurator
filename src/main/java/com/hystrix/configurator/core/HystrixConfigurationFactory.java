package com.hystrix.configurator.core;

import com.hystrix.configurator.config.HystrixCommandConfig;
import com.hystrix.configurator.config.HystrixConfig;
import com.hystrix.configurator.config.HystrixDefaultConfig;
import com.hystrix.configurator.config.ThreadPoolConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author phaneesh
 */
@Slf4j
public class HystrixConfigurationFactory {

    private final HystrixConfig config;

    @Getter
    private HystrixDefaultConfig defaultConfig;

    private ConcurrentHashMap<String, HystrixCommand.Setter> hystrixConfigCache;

    private static HystrixConfigurationFactory factory;

    private HystrixConfigurationFactory(final HystrixConfig config) {
        this.config = config;
    }

    public static void init(final HystrixConfig config) {
        if (factory == null) {
            factory = new HystrixConfigurationFactory(config);
        }
        factory.setup();
    }

    private void setup() {
        if (config == null) {
            return;
        }

        validateUniquePools(config);
        validateUniqueCommands(config);
        defaultConfig = config.getDefaultConfig();

        registerDefaultProperties(defaultConfig);

        Map<String, ThreadPoolConfig> poolConfigs = new HashMap<>();
        if (config.getPools() != null) {
            config.getPools().forEach(pool -> {
                poolConfigs.put(pool.getPool(), pool);
                registerPoolProperties(pool);
                log.info("registered pool: {}", pool);
            });
        }

        hystrixConfigCache = new ConcurrentHashMap<>();
        config.getCommands()
                .forEach(commandConfig -> {
                    if (commandConfig.getCircuitBreaker() == null) {
                        commandConfig.setCircuitBreaker(defaultConfig.getCircuitBreaker());
                    }
                    if (commandConfig.getMetrics() == null) {
                        commandConfig.setMetrics(defaultConfig.getMetrics());
                    }
                    if (commandConfig.getThreadPool() == null) {
                        commandConfig.setThreadPool(defaultConfig.getThreadPool());
                    }
                    registerCommand(defaultConfig, poolConfigs, commandConfig);
                    log.info("registered command: {}", commandConfig.getName());
                });
    }

    private void registerDefaultProperties(HystrixDefaultConfig defaultConfig) {
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.threadpool.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.strategy", defaultConfig.getThreadPool().isSemaphoreIsolation() ? "SEMAPHORE" : "THREAD");
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.thread.timeoutInMilliseconds", defaultConfig.getThreadPool().getTimeout());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.timeout.enabled", true);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.thread.interruptOnTimeout", true);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests", defaultConfig.getThreadPool().getConcurrency());

        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.enabled", true);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.requestVolumeThreshold", defaultConfig.getCircuitBreaker().getAcceptableFailuresInWindow());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.errorThresholdPercentage", defaultConfig.getCircuitBreaker().getErrorThreshold());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds", defaultConfig.getCircuitBreaker().getWaitTimeBeforeRetry());

        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingStats.timeInMilliseconds", defaultConfig.getMetrics().getStatsTimeInMillis());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingStats.numBuckets", defaultConfig.getMetrics().getNumBucketSize());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingPercentile.enabled", true);
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingPercentile.timeInMilliseconds", defaultConfig.getMetrics().getPercentileTimeInMillis());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingPercentile.numBuckets", defaultConfig.getMetrics().getNumBucketSize());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.rollingPercentile.bucketSize", defaultConfig.getMetrics().getPercentileBucketSize());
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.metrics.healthSnapshot.intervalInMilliseconds", defaultConfig.getMetrics().getHealthCheckInterval());
    }

    private void registerPoolProperties(ThreadPoolConfig pool) {
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.threadpool.%s.coreSize", pool.getPool()), pool.getConcurrency());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.threadpool.%s.maxQueueSize", pool.getPool()), pool.getMaxRequestQueueSize());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.threadpool.%s.queueSizeRejectionThreshold", pool.getPool()), pool.getDynamicRequestQueueSize());
    }

    private void registerCommand(HystrixDefaultConfig defaultConfig,
                                 Map<String, ThreadPoolConfig> poolConfigs,
                                 HystrixCommandConfig commandConfig) {
        // First check if a specific pool needs to be used here
        String pool = commandConfig.getThreadPool() != null ? commandConfig.getThreadPool().getPool() : null;
        if (pool == null || defaultConfig.getThreadPool().getPool().equalsIgnoreCase(pool)) {
            pool = defaultConfig.getThreadPool().getPool();
            log.info("using pool: default for command: {}", commandConfig.getName());
        } else {
            if (!poolConfigs.containsKey(pool)) {
                log.warn("unknown pool: {} for command: {} using pool: default", pool, commandConfig.getName());
                pool = defaultConfig.getThreadPool().getPool();
            } else {
                log.info("using pool: {} for command: {}", pool, commandConfig.getName());
            }
        }

        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.coreSize", commandConfig.getName()), commandConfig.getThreadPool().getConcurrency());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.maxQueueSize", commandConfig.getName()), commandConfig.getThreadPool().getMaxRequestQueueSize());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.queueSizeRejectionThreshold", commandConfig.getName()), commandConfig.getThreadPool().getDynamicRequestQueueSize());

        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.execution.isolation.strategy", commandConfig.getName()), commandConfig.getThreadPool().isSemaphoreIsolation() ? "SEMAPHORE" : "THREAD");
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.execution.thread.timeoutInMilliseconds", commandConfig.getName()), commandConfig.getThreadPool().getTimeout());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.execution.timeout.enabled", commandConfig.getName()), true);
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.execution.isolation.thread.interruptOnTimeout", commandConfig.getName()), true);
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.execution.isolation.semaphore.maxConcurrentRequests", commandConfig.getName()), commandConfig.getThreadPool().getConcurrency());

        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.circuitBreaker.enabled", commandConfig.getName()), true);
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.circuitBreaker.requestVolumeThreshold", commandConfig.getName()), commandConfig.getCircuitBreaker().getAcceptableFailuresInWindow());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.circuitBreaker.errorThresholdPercentage", commandConfig.getName()), commandConfig.getCircuitBreaker().getErrorThreshold());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.circuitBreaker.sleepWindowInMilliseconds", commandConfig.getName()), commandConfig.getCircuitBreaker().getWaitTimeBeforeRetry());

        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingStats.timeInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getStatsTimeInMillis());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingStats.numBuckets", commandConfig.getName()), commandConfig.getMetrics().getNumBucketSize());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.enabled", commandConfig.getName()), true);
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.timeInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getPercentileTimeInMillis());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.numBuckets", commandConfig.getName()), commandConfig.getMetrics().getNumBucketSize());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.bucketSize", commandConfig.getName()), commandConfig.getMetrics().getPercentileBucketSize());
        ConfigurationManager.getConfigInstance().setProperty(String.format("hystrix.command.%s.metrics.healthSnapshot.intervalInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getHealthCheckInterval());

        // Register in cache
        hystrixConfigCache.put(commandConfig.getName(), HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandConfig.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandConfig.getName()))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(pool))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withExecutionIsolationStrategy(commandConfig.getThreadPool().isSemaphoreIsolation() ? HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE : HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                                .withExecutionIsolationSemaphoreMaxConcurrentRequests(commandConfig.getThreadPool().getConcurrency())
                                .withFallbackIsolationSemaphoreMaxConcurrentRequests(commandConfig.getThreadPool().getConcurrency())
                                .withFallbackEnabled(commandConfig.isFallbackEnabled())
                                .withCircuitBreakerErrorThresholdPercentage(commandConfig.getCircuitBreaker().getErrorThreshold())
                                .withCircuitBreakerRequestVolumeThreshold(commandConfig.getCircuitBreaker().getAcceptableFailuresInWindow())
                                .withCircuitBreakerSleepWindowInMilliseconds(commandConfig.getCircuitBreaker().getWaitTimeBeforeRetry())
                                .withExecutionTimeoutInMilliseconds(commandConfig.getThreadPool().getTimeout())
                                .withMetricsHealthSnapshotIntervalInMilliseconds(commandConfig.getMetrics().getHealthCheckInterval())
                                .withMetricsRollingPercentileBucketSize(commandConfig.getMetrics().getPercentileBucketSize())
                                .withMetricsRollingPercentileWindowInMilliseconds(commandConfig.getMetrics().getPercentileTimeInMillis())
                )
                .andThreadPoolPropertiesDefaults(
                        HystrixThreadPoolProperties.Setter()
                                .withCoreSize(commandConfig.getThreadPool().getConcurrency())
                                .withMaxQueueSize(commandConfig.getThreadPool().getMaxRequestQueueSize())
                                .withQueueSizeRejectionThreshold(commandConfig.getThreadPool().getDynamicRequestQueueSize())
                                .withMetricsRollingStatisticalWindowBuckets(commandConfig.getMetrics().getNumBucketSize())
                                .withMetricsRollingStatisticalWindowInMilliseconds(commandConfig.getMetrics().getStatsTimeInMillis())
                ));
    }

    private void registerCommand(String command) {
        HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                .name(command)
                .threadPool(defaultConfig.getThreadPool())
                .metrics(defaultConfig.getMetrics())
                .circuitBreaker(defaultConfig.getCircuitBreaker())
                .fallbackEnabled(false)
                .build();
        registerCommand(defaultConfig, Collections.emptyMap(), commandConfig);
    }

    private void validateUniqueCommands(HystrixConfig config) {
        if (config.getPools() == null) {
            return;
        }

        AtomicBoolean duplicatePresent = new AtomicBoolean(false);
        config.getCommands().stream()
                .collect(Collectors.groupingBy(HystrixCommandConfig::getName, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .forEach(x -> {
                    log.warn("Duplicate command configuration command:{}", x.getKey());
                    duplicatePresent.set(true);
                });
        if (duplicatePresent.get()) {
            throw new RuntimeException("Duplicate Command Configurations");
        }
    }

    private void validateUniquePools(HystrixConfig config) {
        if (config.getPools() == null) {
            return;
        }

        AtomicBoolean duplicatePresent = new AtomicBoolean(false);
        config.getPools().stream()
                .collect(Collectors.groupingBy(ThreadPoolConfig::getPool, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .forEach(x -> {
                    log.warn("Duplicate pool configuration pool:{}", x.getKey());
                    duplicatePresent.set(true);
                });
        if (duplicatePresent.get()) {
            throw new RuntimeException("Duplicate Pool Configurations");
        }
    }

    public static HystrixCommand.Setter getCommandConfiguration(final String key) {
        if (factory == null) throw new IllegalStateException("Factory not initialized");
        if (!factory.hystrixConfigCache.containsKey(key)) {
            factory.registerCommand(key);
        }
        return factory.hystrixConfigCache.get(key);
    }
}
