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

import javax.validation.constraints.Max;

/**
 * @author phaneesh
 */
@Data
@NoArgsConstructor
public class MetricsConfig {

    @Max(60000)
    private int statsTimeInMillis = 60000;

    private int healthCheckInterval = 500;

    private int percentileTimeInMillis = 60000;

    private int percentileBucketSize = 100;

    private int numBucketSize = 100;

    @Builder
    public MetricsConfig(int statsTimeInMillis, int healthCheckInterval, int percentileTimeInMillis, int percentileBucketSize, int numBucketSize) {
        this.statsTimeInMillis = statsTimeInMillis;
        this.healthCheckInterval = healthCheckInterval;
        this.percentileTimeInMillis = percentileTimeInMillis;
        this.percentileBucketSize = percentileBucketSize;
        this.numBucketSize = numBucketSize;
    }

    //Default values
    public static class MetricsConfigBuilder {

        private int statsTimeInMillis = 60000;

        private int healthCheckInterval = 500;

        private int percentileTimeInMillis = 60000;

        private int percentileBucketSize = 100;

        private int numBucketSize = 100;

    }
}
