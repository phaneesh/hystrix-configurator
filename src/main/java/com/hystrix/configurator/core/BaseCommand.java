package com.hystrix.configurator.core;

import com.hystrix.configurator.config.HystrixCommandConfig;
import com.netflix.hystrix.HystrixCommand;

/**
 * @author phaneesh
 */
public abstract class BaseCommand<T> extends HystrixCommand<T> {

    public BaseCommand(final String name) {
        super(HystrixConfigurationFactory.getCommandConfiguration(name));
    }

    public BaseCommand(final String name, final HystrixCommandConfig commandConfig) {
        super(HystrixConfigurationFactory.getOrSetCommandConfiguration(name, commandConfig));
    }

}
