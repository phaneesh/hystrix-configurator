package com.hystrix.configurator.core;

import com.hystrix.configurator.config.*;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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
        factory = new HystrixConfigurationFactory(config);
        factory.setup();
    }

    private void setup() {
        validateUniqueCommands(config);
        defaultConfig = config.getDefaultConfig();
        commandCache = new ConcurrentHashMap<>();
        poolCache = new ConcurrentHashMap<>();

        registerDefaultProperties(defaultConfig);

        if (config.getPools() == null){
            config.setPools(new HashMap<>());
        }

        validateUniquePools(config.getPools());
        config.setPools(new TreeMap<String, ThreadPoolConfig>(String.CASE_INSENSITIVE_ORDER) {{
            putAll(config.getPools());
        }});

        config.getPools().forEach((pool, poolConfig) -> {
            configureThreadPoolIfMissing(globalPoolId(pool), poolConfig);
        });

        config.getCommands()
                .forEach(commandConfig -> {
                    commandConfig.setName(sanitizeString(commandConfig.getName()));

                    if (commandConfig.getCircuitBreaker() == null) {
                        commandConfig.setCircuitBreaker(defaultConfig.getCircuitBreaker());
                    }

                    if (commandConfig.getMetrics() == null) {
                        commandConfig.setMetrics(defaultConfig.getMetrics());
                    }

                    if (commandConfig.getThreadPool() == null) {
                        commandConfig.setThreadPool(defaultConfig.getThreadPool());
                    }
                    registerCommandProperties(defaultConfig, commandConfig);
                    log.info("registered command: {}", commandConfig.getName());
                });
    }

    private String globalPoolId(String pool) {
        return sanitizeString(String.format("global_%s", pool));
    }

    private String commandPoolId(String commandName) {
        return sanitizeString(String.format("command_%s", commandName));
    }

    private void registerDefaultProperties(HystrixDefaultConfig defaultConfig) {
        configureProperty("hystrix.threadpool.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        configureProperty("hystrix.threadpool.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        configureProperty("hystrix.threadpool.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        configureProperty("hystrix.command.default.coreSize", defaultConfig.getThreadPool().getConcurrency());
        configureProperty("hystrix.command.default.maxQueueSize", defaultConfig.getThreadPool().getMaxRequestQueueSize());
        configureProperty("hystrix.command.default.queueSizeRejectionThreshold", defaultConfig.getThreadPool().getDynamicRequestQueueSize());

        configureProperty("hystrix.command.default.execution.isolation.strategy", "THREAD");
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

    private void registerCommandProperties(String group, String command) {
        val threadPool = config.getPools().containsKey(sanitizeString(group))
                ? toCommandThreadPool(group)
                : defaultConfig.getThreadPool();

        HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                .name(command)
                .semaphoreIsolation(false)
                .threadPool(threadPool)
                .metrics(defaultConfig.getMetrics())
                .circuitBreaker(defaultConfig.getCircuitBreaker())
                .fallbackEnabled(false)
                .build();
        registerCommandProperties(defaultConfig, commandConfig);
    }

    private void registerCommandProperties(String command) {
        HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                .name(command)
                .semaphoreIsolation(false)
                .threadPool(defaultConfig.getThreadPool())
                .metrics(defaultConfig.getMetrics())
                .circuitBreaker(defaultConfig.getCircuitBreaker())
                .fallbackEnabled(false)
                .build();
        registerCommandProperties(defaultConfig, commandConfig);
    }

    private void registerCommandProperties(HystrixDefaultConfig defaultConfig, HystrixCommandConfig commandConfig) {
        val command = commandConfig.getName();

        val semaphoreIsolation = commandConfig.getThreadPool().isSemaphoreIsolation();

        val isolationStrategy = semaphoreIsolation ? HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE
                : HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;


        configureProperty(String.format("hystrix.command.%s.execution.isolation.strategy", command), isolationStrategy.name());
        configureProperty(String.format("hystrix.command.%s.execution.timeout.enabled", command), true);
        configureProperty(String.format("hystrix.command.%s.execution.thread.timeoutInMilliseconds", command), commandConfig.getThreadPool().getTimeout());
        configureProperty(String.format("hystrix.command.%s.execution.isolation.thread.interruptOnTimeout", command), true);

        if (semaphoreIsolation) {
            configureProperty(String.format("hystrix.command.%s.execution.isolation.semaphore.maxConcurrentRequests", command), commandConfig.getThreadPool().getConcurrency());
        }

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
                .withExecutionIsolationStrategy(isolationStrategy)
                .withFallbackEnabled(commandConfig.isFallbackEnabled())
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerErrorThresholdPercentage(commandConfig.getCircuitBreaker().getErrorThreshold())
                .withCircuitBreakerRequestVolumeThreshold(commandConfig.getCircuitBreaker().getAcceptableFailuresInWindow())
                .withCircuitBreakerSleepWindowInMilliseconds(commandConfig.getCircuitBreaker().getWaitTimeBeforeRetry())
                .withExecutionTimeoutInMilliseconds(commandConfig.getThreadPool().getTimeout())
                .withMetricsHealthSnapshotIntervalInMilliseconds(commandConfig.getMetrics().getHealthCheckInterval())
                .withMetricsRollingPercentileBucketSize(commandConfig.getMetrics().getPercentileBucketSize())
                .withMetricsRollingPercentileWindowInMilliseconds(commandConfig.getMetrics().getPercentileTimeInMillis());

        if (semaphoreIsolation) {
            commandProperties.withExecutionIsolationSemaphoreMaxConcurrentRequests(commandConfig.getThreadPool().getConcurrency())
                    .withFallbackIsolationSemaphoreMaxConcurrentRequests(commandConfig.getThreadPool().getConcurrency());
        }

        HystrixCommand.Setter commandConfiguration = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandConfig.getName()))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandConfig.getName()))
                .andCommandPropertiesDefaults(commandProperties);

        if (!semaphoreIsolation) {
            val globalPool = commandConfig.getThreadPool() != null ? commandConfig.getThreadPool().getPool() : null;
            /*
                If an explicit pool is defined, it should be present in pool list otherwise
                a pool with name same as command would be defined
            */
            HystrixThreadPoolProperties.Setter poolConfiguration;
            val poolName = globalPool != null ? globalPoolId(globalPool) : commandPoolId(command);
            if (globalPool != null) {
                poolConfiguration = poolCache.get(poolName);
                if (poolConfiguration == null) {
                    val message = String.format("Undefined global pool [%s] used in command [%s]", globalPool, command);
                    log.error(message);
                    throw new RuntimeException(message);
                }
            } else {
                configureThreadPoolIfMissing(poolName, toPoolConfig(commandConfig.getThreadPool()));
            }

            commandConfiguration.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(poolName));
        }
        commandCache.put(commandConfig.getName(), commandConfiguration);
    }

    private ThreadPoolConfig toPoolConfig(CommandThreadPoolConfig threadPool) {
        return ThreadPoolConfig.builder()
                .concurrency(threadPool.getConcurrency())
                .dynamicRequestQueueSize(threadPool.getDynamicRequestQueueSize())
                .maxRequestQueueSize(threadPool.getMaxRequestQueueSize())
                .build();
    }

    private void configureThreadPoolIfMissing(String poolName, ThreadPoolConfig poolConfig) {
        if (poolCache.containsKey(poolName)) {
            return;
        }

        configureProperty(String.format("hystrix.threadpool.%s.coreSize", poolName), poolConfig.getConcurrency());
        configureProperty(String.format("hystrix.threadpool.%s.maximumSize", poolName), poolConfig.getConcurrency());
        configureProperty(String.format("hystrix.threadpool.%s.maxQueueSize", poolName), poolConfig.getMaxRequestQueueSize());
        configureProperty(String.format("hystrix.threadpool.%s.queueSizeRejectionThreshold", poolName), poolConfig.getDynamicRequestQueueSize());

        poolCache.put(poolName, HystrixThreadPoolProperties.Setter()
                .withCoreSize(poolConfig.getConcurrency())
                .withMaximumSize(poolConfig.getConcurrency())
                .withMaxQueueSize(poolConfig.getMaxRequestQueueSize())
                .withQueueSizeRejectionThreshold(poolConfig.getDynamicRequestQueueSize()));
    }

    private void validateUniquePools(Map<String, ThreadPoolConfig> pools) {
        AtomicBoolean duplicatePresent = new AtomicBoolean(false);
        pools.entrySet()
                .stream()
                .collect(Collectors.groupingBy(x->sanitizeString(x.getKey()), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(x -> x.getValue() > 1)
                .forEach(x->{
                    log.error("Duplicate group configuration for group [{}]", x.getKey());
                    duplicatePresent.set(true);
                });
        if (duplicatePresent.get()) {
            throw new RuntimeException("Duplicate Hystrix Group Configurations");
        }
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
                    log.error("Duplicate command configuration for command [{}]", x.getKey());
                    duplicatePresent.set(true);
                });
        if (duplicatePresent.get()) {
            throw new RuntimeException("Duplicate Hystrix Command Configurations");
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
        val sanitizedCommandName = sanitizeString(key);
        if (factory == null) throw new IllegalStateException("Factory not initialized");
        if (!factory.commandCache.containsKey(sanitizedCommandName)) {
            factory.registerCommandProperties(sanitizedCommandName);
        }
        return factory.commandCache.get(sanitizedCommandName);
    }

    public static HystrixCommand.Setter getCommandConfiguration(final String group, final String command) {
        val sanitizedGroupName = sanitizeString(group);
        val sanitizedCommandName = sanitizeString(command);
        if (factory == null) throw new IllegalStateException("Factory not initialized");
        if (!factory.commandCache.containsKey(sanitizedCommandName)) {
            factory.registerCommandProperties(sanitizedGroupName, sanitizedCommandName);
        }
        return factory.commandCache.get(sanitizedCommandName);
    }

    public static ConcurrentHashMap<String, HystrixCommand.Setter> getCommandCache() {
        return factory.commandCache;
    }

    public static ConcurrentHashMap<String, HystrixThreadPoolProperties.Setter> getPoolCache() {
        return factory.poolCache;
    }

    private CommandThreadPoolConfig toCommandThreadPool(String pool) {
        return CommandThreadPoolConfig.builder()
                .pool(pool)
                .build();
    }

    private static String sanitizeString(String str){
        return StringUtils.lowerCase(str);
    }

}