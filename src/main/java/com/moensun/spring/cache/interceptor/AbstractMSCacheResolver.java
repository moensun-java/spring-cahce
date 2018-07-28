package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:22
 */
public abstract class AbstractMSCacheResolver implements MSCacheResolver, InitializingBean {

    private RedisCacheManager cacheManager;


    protected AbstractMSCacheResolver() {
    }

    protected AbstractMSCacheResolver(RedisCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }


    /**
     * Set the {@link RedisCacheManager} that this instance should use.
     */
    public void setCacheManager(RedisCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Return the {@link RedisCacheManager} that this instance uses.
     */
    public RedisCacheManager getCacheManager() {
        return this.cacheManager;
    }

    @Override
    public void afterPropertiesSet()  {
        Assert.notNull(this.cacheManager, "CacheManager is required");
    }


    @Override
    public Collection<? extends MSCache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<String> cacheNames = getCacheNames(context);
        if (cacheNames == null) {
            return Collections.emptyList();
        }
        else {
            Collection<MSCache> result = new ArrayList<MSCache>();
            for (String cacheName : cacheNames) {
                MSCache cache = getCacheManager().getCache(cacheName);
                if (cache == null) {
                    throw new IllegalArgumentException("Cannot find cache named '" +
                            cacheName + "' for " + context.getOperation());
                }
                result.add(cache);
            }
            return result;
        }
    }

    /**
     * Provide the name of the cache(s) to resolve against the current cache manager.
     * <p>It is acceptable to return {@code null} to indicate that no cache could
     * be resolved for this invocation.
     * @param context the context of the particular invocation
     * @return the cache name(s) to resolve, or {@code null} if no cache should be resolved
     */
    protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
