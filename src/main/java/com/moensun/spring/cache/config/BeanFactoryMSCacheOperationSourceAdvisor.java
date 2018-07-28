package com.moensun.spring.cache.config;

import com.moensun.spring.cache.interceptor.MSCacheOperationSourcePointcut;
import com.moensun.spring.cache.operation.MSCacheOperationSource;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午1:55
 */
public class BeanFactoryMSCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private MSCacheOperationSource cacheOperationSource;

    private final MSCacheOperationSourcePointcut pointcut = new MSCacheOperationSourcePointcut() {
        @Override
        protected MSCacheOperationSource getCacheOperationSource() {
            return cacheOperationSource;
        }
    };


    /**
     * Set the cache operation attribute source which is used to find cache
     * attributes. This should usually be identical to the source reference
     * set on the cache interceptor itself.
     */
    public void setCacheOperationSource(MSCacheOperationSource cacheOperationSource) {
        this.cacheOperationSource = cacheOperationSource;
    }

    /**
     * Set the {@link ClassFilter} to use for this pointcut.
     * Default is {@link ClassFilter#TRUE}.
     */
    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
