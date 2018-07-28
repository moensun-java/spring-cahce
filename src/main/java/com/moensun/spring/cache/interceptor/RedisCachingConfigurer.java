package com.moensun.spring.cache.interceptor;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:48
 */
public interface RedisCachingConfigurer {

    RedisCacheManager cacheManager();

    MSCacheResolver cacheResolver();

    KeyGenerator keyGenerator();

    CacheErrorHandler errorHandler();

}
