package com.fclassroom.ssp.spring.cache.config;

import com.fclassroom.ssp.spring.cache.interceptor.MSCacheInterceptor;
import com.fclassroom.ssp.spring.cache.operation.AnnotationMSCacheOperationSource;
import com.fclassroom.ssp.spring.cache.operation.MSCacheOperationSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午3:00
 */
@Configuration
public class ProxyMSCachingConfiguration extends AbstractMSCachingConfiguration {

    @Bean(name = "com.fclassroom.ssp.spring.cache.config.RedisInternalCacheAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryMSCacheOperationSourceAdvisor cacheAdvisor() {
        BeanFactoryMSCacheOperationSourceAdvisor advisor =
                new BeanFactoryMSCacheOperationSourceAdvisor();
        advisor.setCacheOperationSource(cacheOperationSource());
        advisor.setAdvice(cacheInterceptor());
        advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
        return advisor;
    }

    @Bean(name = "com.fclassroom.ssp.spring.cache.operation.RedisCacheOperationSource")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MSCacheOperationSource cacheOperationSource() {
        return new AnnotationMSCacheOperationSource();
    }

    @Bean(name = "com.fclassroom.ssp.spring.cache.interceptor.RedisCacheInterceptor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public MSCacheInterceptor cacheInterceptor() {
        MSCacheInterceptor interceptor = new MSCacheInterceptor();
        interceptor.setCacheOperationSources(cacheOperationSource());
        if (this.cacheResolver != null) {
            interceptor.setCacheResolver(this.cacheResolver);
        }
        else if (this.cacheManager != null) {
            interceptor.setCacheManager(this.cacheManager);
        }
        if (this.keyGenerator != null) {
            interceptor.setKeyGenerator(this.keyGenerator);
        }
        if (this.errorHandler != null) {
            interceptor.setErrorHandler(this.errorHandler);
        }
        return interceptor;
    }

}
