package com.moensun.spring.cache;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午6:17
 */
public interface MSCache extends Cache {

    <T> T hGet(Object key, Object hashKey, Class<T> type);

    <T> T hGet(Object key, Object hashKey, Callable<T> valueLoader);

    void hSet(Object key, Object hashKey, Object value);

    ValueWrapper hGet(Object key, Object hashKey);

    void hEvict(Object key, Object hashKey);

}
