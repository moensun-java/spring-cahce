package com.moensun.spring.cache.interceptor;

import org.springframework.cache.interceptor.CacheOperationInvocationContext;

import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:32
 */
public class SimpleMSCacheResolver extends AbstractMSCacheResolver {

    public SimpleMSCacheResolver() {
    }

    public SimpleMSCacheResolver(RedisCacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        return context.getOperation().getCacheNames();
    }

}
