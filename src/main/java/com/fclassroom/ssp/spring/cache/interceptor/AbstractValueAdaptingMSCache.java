package com.fclassroom.ssp.spring.cache.interceptor;

import com.fclassroom.ssp.spring.cache.MSCache;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.cache.support.SimpleValueWrapper;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:36
 */
public abstract class AbstractValueAdaptingMSCache implements MSCache {

    private final boolean allowNullValues;


    /**
     * Create an {@code AbstractValueAdaptingCache} with the given setting.
     * @param allowNullValues whether to allow for {@code null} values
     */
    protected AbstractValueAdaptingMSCache(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }


    /**
     * Return whether {@code null} values are allowed in this cache.
     */
    public final boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        Object value = lookup(key);
        return toValueWrapper(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object value = fromStoreValue(lookup(key));
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    /**
     * Perform an actual lookup in the underlying store.
     * @param key the key whose associated value is to be returned
     * @return the raw store value for the key
     */
    protected abstract Object lookup(Object key);

    /**
     * Hash
     * @param key
     * @return
     */
    protected abstract Object hLookup(Object key);


    /**
     * Convert the given value from the internal store to a user value
     * returned from the get method (adapting {@code null}).
     * @param storeValue the store value
     * @return the value to return to the user
     */
    protected Object fromStoreValue(Object storeValue) {
        if (this.allowNullValues && storeValue == NullValue.INSTANCE) {
            return null;
        }
        return storeValue;
    }

    /**
     * Convert the given user value, as passed into the put method,
     * to a value in the internal store (adapting {@code null}).
     * @param userValue the given user value
     * @return the value to store
     */
    protected Object toStoreValue(Object userValue) {
        if (this.allowNullValues && userValue == null) {
            return NullValue.INSTANCE;
        }
        return userValue;
    }

    /**
     * Wrap the given store value with a {@link SimpleValueWrapper}, also going
     * through {@link #fromStoreValue} conversion. Useful for {@link #get(Object)}
     * and {@link #putIfAbsent(Object, Object)} implementations.
     * @param storeValue the original value
     * @return the wrapped value
     */
    protected ValueWrapper toValueWrapper(Object storeValue) {
        return (storeValue != null ? new SimpleMSValueWrapper(fromStoreValue(storeValue)) : null);
    }

}
