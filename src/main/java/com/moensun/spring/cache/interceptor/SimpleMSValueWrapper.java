package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:39
 */
public class SimpleMSValueWrapper implements MSCache.ValueWrapper {

    private final Object value;


    /**
     * Create a new SimpleValueWrapper instance for exposing the given value.
     * @param value the value to expose (may be {@code null})
     */
    public SimpleMSValueWrapper(Object value) {
        this.value = value;
    }


    /**
     * Simply returns the value as given at construction time.
     */
    @Override
    public Object get() {
        return this.value;
    }

}
