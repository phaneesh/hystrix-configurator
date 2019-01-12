package com.hystrix.configurator.core;

import com.hystrix.configurator.config.HystrixCommandConfig;
import com.hystrix.configurator.config.HystrixConfig;
import com.hystrix.configurator.config.HystrixDefaultConfig;
import com.hystrix.configurator.config.ThreadPoolConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

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

    private ConcurrentHashMap<String, HystrixCommand.Setter> commandCache;

    private ConcurrentHashMap<String, HystrixThreadPoolProperties.Setter> poolCache;

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
        validateUniquePools(config);
        validateUniqueCommands(config);
        defaultConfig = config.getDefaultConfig();

        registerDefaultProperties(defaultConfig);

        Map<String, ThreadPoolConfig> poolConfigs = new HashMap<>();
        if (config.getPools() != null) {
            config.getPools().forEach(pool -> {
                poolConfigs.put(pool.getPool(), pool);
                registerThreadPoolProperties(pool);
                log.info("registered pool: {}", pool);
            });
        }

        commandCache = new ConcurrentHashMap<>();
        poolCache = new ConcurrentHashMap<>();

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
                    registerCommandProperties(defaultConfig, poolConfigs, commandConfig);
                    log.info("registered command: {}", commandConfig.getName());
                });
    }

    private void registerDefaultProperties(HystrixDefaultConfig defaultConfig) {
        configureProperty("hystrix.threadpool.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        configureProperty("hystrix.threadpool.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        configureProperty("hystrix.threadpool.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        configureProperty("hystrix.command.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        configureProperty("hystrix.command.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        configureProperty("hystrix.command.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        configureProperty("hystrix.command.default.execution.isolation.strategy", defaultConfig.getThreadPool().isSemaphoreIsolation() ? "SEMAPHORE" : "THREAD");
        configureProperty("hystrix.command.default.execution.thread.timeoutInMilliseconds", defaultConfig.getThreadPool().getTimeout());
        configureProperty("hystrix.command.default.execution.timeout.enabled", true);
        configureProperty("hystrix.command.default.execution.isolation.thread.interruptOnTimeout", true);
        configureProperty("hystrix.command.default.execution.isolation.semaphore.maxConcurrentRequests", defaultConfig.getThreadPool().getConcurrency());

        configureProperty("hystrix.command.default.circuitBreaker.enabled", true);
        configureProperty("hystrix.command.default.circuitBreaker.requestVolumeThreshold", defaultConfig.getCircuitBreaker().getAcceptableFailuresInWindow());
        configureProperty("hystrix.command.default.circuitBreaker.errorThresholdPercentage", defaultConfig.getCircuitBreaker().getErrorThreshold());
        configureProperty("hystrix.command.default.circuitBreaker.sleepWindowInMilliseconds", defaultConfig.getCircuitBreaker().getWaitTimeBeforeRetry());

        configureProperty("hystrix.command.default.metrics.rollingStats.timeInMilliseconds", defaultConfig.getMetrics().getStatsTimeInMillis());
        configureProperty("hystrix.command.default.metrics.rollingStats.numBuckets", defaultConfig.getMetrics().getNumBucketSize());
        configureProperty("hystrix.command.default.metrics.rollingPercentile.enabled", true);
        configureProperty("hystrix.command.default.metrics.rollingPercentile.timeInMilliseconds", defaultConfig.getMetrics().getPercentileTimeInMillis());
        configureProperty("hystrix.command.default.metrics.rollingPercentile.numBuckets", defaultConfig.getMetrics().getNumBucketSize());
        configureProperty("hystrix.command.default.metrics.rollingPercentile.bucketSize", defaultConfig.getMetrics().getPercentileBucketSize());
        configureProperty("hystrix.command.default.metrics.healthSnapshot.intervalInMilliseconds", defaultConfig.getMetrics().getHealthCheckInterval());
    }

    private void registerThreadPoolProperties(ThreadPoolConfig pool) {

    }

    private void registerCommandProperties(HystrixDefaultConfig defaultConfig,
                                           Map<String, ThreadPoolConfig> poolConfigs,
                                           HystrixCommandConfig commandConfig) {
        val command = commandConfig.getName();

        configureProperty(String.format("hystrix.command.%s.execution.isolation.strategy", command), commandConfig.getThreadPool().isSemaphoreIsolation() ? "SEMAPHORE" : "THREAD");
        configureProperty(String.format("hystrix.command.%s.execution.thread.timeoutInMilliseconds", command), commandConfig.getThreadPool().getTimeout());
        configureProperty(String.format("hystrix.command.%s.execution.timeout.enabled", command), true);
        configureProperty(String.format("hystrix.command.%s.execution.isolation.thread.interruptOnTimeout", command), true);
        configureProperty(String.format("hystrix.command.%s.execution.isolation.semaphore.maxConcurrentRequests", command), commandConfig.getThreadPool().getConcurrency());

        configureProperty(String.format("hystrix.command.%s.circuitBreaker.enabled", commandConfig.getName()), true);
        configureProperty(String.format("hystrix.command.%s.circuitBreaker.requestVolumeThreshold", commandConfig.getName()), commandConfig.getCircuitBreaker().getAcceptableFailuresInWindow());
        configureProperty(String.format("hystrix.command.%s.circuitBreaker.errorThresholdPercentage", commandConfig.getName()), commandConfig.getCircuitBreaker().getErrorThreshold());
        configureProperty(String.format("hystrix.command.%s.circuitBreaker.sleepWindowInMilliseconds", commandConfig.getName()), commandConfig.getCircuitBreaker().getWaitTimeBeforeRetry());

        configureProperty(String.format("hystrix.command.%s.metrics.rollingStats.timeInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getStatsTimeInMillis());
        configureProperty(String.format("hystrix.command.%s.metrics.rollingStats.numBuckets", commandConfig.getName()), commandConfig.getMetrics().getNumBucketSize());
        configureProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.enabled", commandConfig.getName()), true);
        configureProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.timeInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getPercentileTimeInMillis());
        configureProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.numBuckets", commandConfig.getName()), commandConfig.getMetrics().getNumBucketSize());
        configureProperty(String.format("hystrix.command.%s.metrics.rollingPercentile.bucketSize", commandConfig.getName()), commandConfig.getMetrics().getPercentileBucketSize());
        configureProperty(String.format("hystrix.command.%s.metrics.healthSnapshot.intervalInMilliseconds", commandConfig.getName()), commandConfig.getMetrics().getHealthCheckInterval());

        val commandProperties = HystrixCommandProperties.Setter()
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
                .withMetricsRollingPercentileWindowInMilliseconds(commandConfig.getMetrics().getPercentileTimeInMillis());

        HystrixCommand.Setter commandConfiguration = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandConfig.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandConfig.getName()))
                .andCommandPropertiesDefaults(commandProperties);

        if (!commandConfig.getThreadPool().isSemaphoreIsolation()) {
            val explicitPool = commandConfig.getThreadPool() != null ? commandConfig.getThreadPool().getPool() : null;
            /*
                If an explicit pool is defined, it should be present in pool list otherwise
                a pool with name same as command would be defined
            */
            if (explicitPool != null && !poolConfigs.containsKey(explicitPool)) {
                val message = String.format("Undefined explicit pool [%s] used in command [%s]", explicitPool, command);
                log.error(message);
                throw new RuntimeException(message);
            }
            val effectivePool = explicitPool != null ? explicitPool : command;
            val poolConfig = explicitPool != null ? poolConfigs.get(explicitPool) : defaultConfig.getThreadPool();
            val poolPrefix = explicitPool != null ? "pool" : "com";
            val pool = String.format("%s_%s", poolPrefix, effectivePool);
            log.info("using pool: {} with prefix: {} for command: {}", effectivePool, poolPrefix, command);

            val poolConfiguration = getOrCreateThreadPool(pool, defaultConfig, poolConfig);
            commandConfiguration
                    .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(pool))
                    .andThreadPoolPropertiesDefaults(poolConfiguration);
        }
        commandCache.put(commandConfig.getName(), commandConfiguration);
    }

    private HystrixThreadPoolProperties.Setter getOrCreateThreadPool(String pool,
                                                                     HystrixDefaultConfig defaultConfig,
                                                                     ThreadPoolConfig poolConfig) {
        if (poolCache.containsKey(pool)) {
            return poolCache.get(pool);
        }

        configureProperty(String.format("hystrix.threadpool.%s.coreSize", pool), poolConfig.getConcurrency());
        configureProperty(String.format("hystrix.threadpool.%s.maximumSize", pool), poolConfig.getConcurrency());
        configureProperty(String.format("hystrix.threadpool.%s.maxQueueSize", pool), poolConfig.getMaxRequestQueueSize());
        configureProperty(String.format("hystrix.threadpool.%s.queueSizeRejectionThreshold", pool), poolConfig.getDynamicRequestQueueSize());

        HystrixThreadPoolProperties.Setter poolHystrixSetter = HystrixThreadPoolProperties.Setter()
                .withCoreSize(poolConfig.getConcurrency())
                .withMaximumSize(poolConfig.getConcurrency())
                .withMaxQueueSize(poolConfig.getMaxRequestQueueSize())
                .withQueueSizeRejectionThreshold(poolConfig.getDynamicRequestQueueSize());
        poolCache.put(pool, poolHystrixSetter);
        return poolHystrixSetter;
    }

    private void registerCommandProperties(String command) {
        HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                .name(command)
                .threadPool(defaultConfig.getThreadPool())
                .metrics(defaultConfig.getMetrics())
                .circuitBreaker(defaultConfig.getCircuitBreaker())
                .fallbackEnabled(false)
                .build();
        registerCommandProperties(defaultConfig, Collections.emptyMap(), commandConfig);
    }

    private void validateUniqueCommands(HystrixConfig config) {
        if (config.getCommands() == null) {
            return;
        }

        AtomicBoolean duplicatePresent = new AtomicBoolean(false);
        config.getCommands().stream()
                .collect(Collectors.groupingBy(HystrixCommandConfig::getName, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .forEach(x -> {
                    log.warn("Duplicate command configuration for command [{}]", x.getKey());
                    duplicatePresent.set(true);
                });
        if (duplicatePresent.get()) {
            throw new RuntimeException("Duplicate Hystrix Command Configurations");
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
            throw new RuntimeException("Duplicate Hystrix Pool Configurations");
        }
    }

    private void configureProperty(String property, int value) {
        ConfigurationManager.getConfigInstance().setProperty(property, value);
    }

    private void configureProperty(String property, String value) {
        ConfigurationManager.getConfigInstance().setProperty(property, value);
    }

    private void configureProperty(String property, boolean value) {
        ConfigurationManager.getConfigInstance().setProperty(property, value);
    }
    

    public static HystrixCommand.Setter getCommandConfiguration(final String key) {
        if (factory == null) throw new IllegalStateException("Factory not initialized");
        if (!factory.commandCache.containsKey(key)) {
            factory.registerCommandProperties(key);
        }
        return factory.commandCache.get(key);
    }
}
