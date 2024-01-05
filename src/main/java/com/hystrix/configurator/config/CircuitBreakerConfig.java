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

/**
 * @author phaneesh
 */
@Data
@NoArgsConstructor
public class CircuitBreakerConfig {

    private int acceptableFailuresInWindow = 20;

    private int waitTimeBeforeRetry = 5000;

    private int errorThreshold = 50;

    private boolean forceClosed;


    @Builder
    public CircuitBreakerConfig(int acceptableFailuresInWindow, int waitTimeBeforeRetry,
        int errorThreshold, boolean forceClosed) {
        this.acceptableFailuresInWindow = acceptableFailuresInWindow;
        this.waitTimeBeforeRetry = waitTimeBeforeRetry;
        this.errorThreshold = errorThreshold;
        this.forceClosed = forceClosed;
    }

    //Default values
    public static class CircuitBreakerConfigBuilder {

        private int acceptableFailuresInWindow = 20;

        private int waitTimeBeforeRetry = 5000;

        private int errorThreshold = 50;

        private boolean forceClosed = false;
    }
}
