package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午6:44
 */
public interface MSCacheResolver extends CacheResolver {

    Collection<? extends MSCache> resolveCaches(CacheOperationInvocationContext<?> context);

}
