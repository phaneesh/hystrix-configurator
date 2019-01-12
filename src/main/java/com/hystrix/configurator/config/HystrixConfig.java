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
import java.util.Collections;
import java.util.List;

/**
 * @author phaneesh
 */
@Data
@NoArgsConstructor
public class HystrixConfig {

    @NotNull
    private HystrixDefaultConfig defaultConfig = HystrixDefaultConfig.builder().build();

    @Valid
    private List<ThreadPoolConfig> pools = Collections.emptyList();

    @Valid
    private List<HystrixCommandConfig> commands = Collections.emptyList();

    @Builder
    public HystrixConfig(HystrixDefaultConfig defaultConfig,
                         List<ThreadPoolConfig> pools,
                         List<HystrixCommandConfig> commands) {
        this.defaultConfig = defaultConfig;
        this.pools = pools;
        this.commands = commands;
    }

    public static class HystrixConfigBuilder {

        private HystrixDefaultConfig defaultConfig = HystrixDefaultConfig.builder().build();

        private List<HystrixCommandConfig> commands = Collections.emptyList();

    }
}
