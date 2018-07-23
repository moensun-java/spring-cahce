package com.fclassroom.ssp.spring.cache.operation;

import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午1:00
 */
public class CompositeMSCacheOperationSource implements MSCacheOperationSource, Serializable {

    private final MSCacheOperationSource[] cacheOperationSources;


    /**
     * Create a new CompositeCacheOperationSource for the given sources.
     * @param cacheOperationSources the CacheOperationSource instances to combine
     */
    public CompositeMSCacheOperationSource(MSCacheOperationSource... cacheOperationSources) {
        Assert.notEmpty(cacheOperationSources, "cacheOperationSources array must not be empty");
        this.cacheOperationSources = cacheOperationSources;
    }

    /**
     * Return the {@code CacheOperationSource} instances that this
     * {@code CompositeCacheOperationSource} combines.
     */
    public final MSCacheOperationSource[] getCacheOperationSources() {
        return this.cacheOperationSources;
    }

    @Override
    public Collection<MSCacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
        Collection<MSCacheOperation> ops = null;

        for (MSCacheOperationSource source : this.cacheOperationSources) {
            Collection<MSCacheOperation> cacheOperations = source.getCacheOperations(method, targetClass);
            if (cacheOperations != null) {
                if (ops == null) {
                    ops = new ArrayList<MSCacheOperation>();
                }

                ops.addAll(cacheOperations);
            }
        }
        return ops;
    }

}
