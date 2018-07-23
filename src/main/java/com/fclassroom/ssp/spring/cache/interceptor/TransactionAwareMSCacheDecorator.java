package com.fclassroom.ssp.spring.cache.interceptor;

import com.fclassroom.ssp.spring.cache.MSCache;
import org.springframework.cache.Cache;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午6:20
 */
public class TransactionAwareMSCacheDecorator implements MSCache {

    private final MSCache targetCache;

    public TransactionAwareMSCacheDecorator(MSCache targetCache) {
        Assert.notNull(targetCache, "Target Cache must not be null");
        this.targetCache = targetCache;
    }

    public MSCache getTargetCache() {
        return this.targetCache;
    }


    @Override
    public <T> T hGet(Object key, Object hashKey, Class<T> type) {
        return this.targetCache.hGet(key,hashKey,type);
    }

    @Override
    public <T> T hGet(Object key, Object hashKey, Callable<T> valueLoader) {
        return this.targetCache.hGet(key,hashKey,valueLoader);
    }

    @Override
    public void hSet(Object key, Object hashKey, Object value) {
        this.targetCache.hSet(key,hashKey,value);
    }

    @Override
    public Cache.ValueWrapper hGet(Object key, Object hashKey) {
        return this.targetCache.hGet(key,hashKey);
    }

    @Override
    public void hEvict(Object key, Object hashKey) {
        this.targetCache.hEvict(key,hashKey);
    }


    @Override
    public String getName() {
        return this.targetCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return this.targetCache.getNativeCache();
    }

    @Override
    public Cache.ValueWrapper get(Object key) {
        return this.targetCache.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return this.targetCache.get(key,type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return this.targetCache.get(key,valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
         this.targetCache.put(key,value);
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(Object key, Object value) {
        return this.putIfAbsent(key,value);
    }


    @Override
    public void evict(Object key) {
        this.targetCache.evict(key);
    }

    @Override
    public void clear() {
        this.targetCache.clear();
    }
}
