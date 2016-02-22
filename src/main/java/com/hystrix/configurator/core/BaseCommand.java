package com.hystrix.configurator.core;

import com.netflix.hystrix.HystrixCommand;

/**
 * @author phaneesh
 */
public abstract class BaseCommand<T> extends HystrixCommand<T> {

    public BaseCommand(final String name) {
        super(HystrixConfigutationFactory.getCommandConfiguration(name));
    }

}
