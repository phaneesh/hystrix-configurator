package com.hystrix.configutator;

import com.hystrix.configurator.config.CircuitBreakerConfig;
import com.hystrix.configurator.config.CommandThreadPoolConfig;
import com.hystrix.configurator.config.HystrixCommandConfig;
import com.hystrix.configurator.config.HystrixConfig;
import com.hystrix.configurator.config.HystrixDefaultConfig;
import com.hystrix.configurator.config.MetricsConfig;
import com.hystrix.configurator.core.BaseCommand;
import com.hystrix.configurator.core.HystrixConfigurationFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
public class HystrixConfigurationDynamicUpdateTest {

    @Before
    public void setup() {
        HystrixConfigurationFactory.init(
                HystrixConfig.builder()
                        .defaultConfig(new HystrixDefaultConfig())
                        .commands(Collections.singletonList(HystrixCommandConfig.builder().name("test").build()))
                        .build());
    }

    private void changeTimeoutAndTest(String commandName) throws ExecutionException, InterruptedException {

        HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                .name("TestCommand")
                .semaphoreIsolation(false)
                .threadPool(CommandThreadPoolConfig.builder().timeout(4000).build())
                .metrics(MetricsConfig.builder().build())
                .circuitBreaker(CircuitBreakerConfig.builder().build())
                .fallbackEnabled(false)
                .build();
        SimpleTestCommand command111 = new SimpleTestCommand(commandConfig);
        String result1 = command111.queue().get();
        Assert.assertTrue(result1.equals("Simple Test"));
    }


    @Test
    public void dynamicConfig() throws ExecutionException, InterruptedException {
        try {
            HystrixCommandConfig commandConfig = HystrixCommandConfig.builder()
                    .name("TestCommand")
                    .semaphoreIsolation(false)
                    .threadPool(CommandThreadPoolConfig.builder().timeout(1000).build())
                    .metrics(MetricsConfig.builder().build())
                    .circuitBreaker(CircuitBreakerConfig.builder().build())
                    .fallbackEnabled(false)
                    .build();

            SimpleTestCommand command3 = new SimpleTestCommand(commandConfig);
            String result = command3.queue().get();
            Assert.assertTrue(result.equals("Simple Test"));
        } catch (Exception e) {
            changeTimeoutAndTest("TestCommand");

        }
    }

    public static class SimpleTestCommand extends BaseCommand<String> {

        HystrixCommandConfig commandConfig;

        public SimpleTestCommand(HystrixCommandConfig commandConfig) {
            super("TestCommand", commandConfig);
        }

        @Override
        protected String run() throws InterruptedException {
            Thread.sleep(2000);
            return "Simple Test";
        }
    }
}
