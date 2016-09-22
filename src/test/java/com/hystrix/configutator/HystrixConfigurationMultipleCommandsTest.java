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
 *
 */

package com.hystrix.configutator;

import com.hystrix.configurator.config.HystrixCommandConfig;
import com.hystrix.configurator.config.HystrixConfig;
import com.hystrix.configurator.config.HystrixDefaultConfig;
import com.hystrix.configurator.core.HystrixConfigurationFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * @author phaneesh
 */
public class HystrixConfigurationMultipleCommandsTest {

    @Before
    public void setup() {
        HystrixConfigurationFactory.init(
                HystrixConfig.builder()
                        .defaultConfig(new HystrixDefaultConfig())
                        .commands(Collections.singletonList(HystrixCommandConfig.builder().name("test").build()))
                        .build());
    }

    @Test
    public void testCommand() throws ExecutionException, InterruptedException {
        SimpleTestCommand1 command1 = new SimpleTestCommand1();
        String result1 = command1.queue().get();
        Assert.assertTrue(result1.equals("Simple Test 1"));

        SimpleTestCommand2 command2 = new SimpleTestCommand2();
        String result2 = command2.queue().get();
        Assert.assertTrue(result2.equals("Simple Test 2"));
    }

    public static class SimpleTestCommand1 extends HystrixCommand<String> {

        public SimpleTestCommand1() {
            super(HystrixCommandGroupKey.Factory.asKey("test"));
        }

        @Override
        protected String run() throws Exception {
            return "Simple Test 1";
        }
    }

    public static class SimpleTestCommand2 extends HystrixCommand<String> {

        public SimpleTestCommand2() {
            super(HystrixCommandGroupKey.Factory.asKey("test"));
        }

        @Override
        protected String run() throws Exception {
            return "Simple Test 2";
        }
    }
}
