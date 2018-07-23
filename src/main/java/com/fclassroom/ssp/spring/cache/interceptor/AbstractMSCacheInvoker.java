package com.fclassroom.ssp.spring.cache.interceptor;

import com.fclassroom.ssp.spring.cache.MSCache;
import org.springframework.cache.interceptor.AbstractCacheInvoker;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:08
 */
public abstract class AbstractMSCacheInvoker extends AbstractCacheInvoker {


    protected MSCache.ValueWrapper doGet(MSCache cache, Object key) {
        try {
            return cache.get(key);
        }
        catch (RuntimeException ex) {
            getErrorHandler().handleCacheGetError(ex, cache, key);
            return null;  // If the exception is handled, return a cache miss
        }
    }

    protected MSCache.ValueWrapper doHGet(MSCache cache, Object key, Object hashkey) {
        try {
            return cache.hGet(key,hashkey);
        }
        catch (RuntimeException ex) {
            getErrorHandler().handleCacheGetError(ex, cache, key);
            return null;  // If the exception is handled, return a cache miss
        }
    }

    void doHSet(MSCache cache, Object key, Object hashKey , Object value){
        try {
            cache.hSet(key,hashKey,value);
        }
        catch (RuntimeException ex) {
            getErrorHandler().handleCacheGetError(ex, cache, key);
        }
    }


    protected void doHEvict(MSCache cache, Object key, Object hashKey) {
        try {
            cache.hEvict(key,hashKey);
        }
        catch (RuntimeException ex) {
            getErrorHandler().handleCacheEvictError(ex, cache, key);
        }
    }


}
