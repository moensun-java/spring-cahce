package com.fclassroom.ssp.spring.cache.interceptor;

import com.fclassroom.ssp.spring.cache.operation.MSCacheOperationSource;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午1:57
 */
public abstract class MSCacheOperationSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        MSCacheOperationSource cas = getCacheOperationSource();
        return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MSCacheOperationSourcePointcut)) {
            return false;
        }
        MSCacheOperationSourcePointcut otherPc = (MSCacheOperationSourcePointcut) other;
        return ObjectUtils.nullSafeEquals(getCacheOperationSource(), otherPc.getCacheOperationSource());
    }

    @Override
    public int hashCode() {
        return MSCacheOperationSourcePointcut.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getCacheOperationSource();
    }


    /**
     * Obtain the underlying {@link MSCacheOperationSource} (may be {@code null}).
     * To be implemented by subclasses.
     */
    protected abstract MSCacheOperationSource getCacheOperationSource();


}
