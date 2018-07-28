package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;
import org.springframework.cache.CacheManager;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:24
 */
public interface RedisCacheManager extends CacheManager {

    MSCache getCache(String name);

}
