package com.fclassroom.ssp.spring.cache.operation;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午12:46
 */
public interface MSCacheOperationSource {

    Collection<MSCacheOperation> getCacheOperations(Method method, Class<?> targetClass);

}
