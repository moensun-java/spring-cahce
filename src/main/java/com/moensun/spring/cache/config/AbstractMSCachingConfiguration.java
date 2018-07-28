package com.moensun.spring.cache.config;

import com.moensun.spring.cache.EnableMSCaching;
import com.moensun.spring.cache.interceptor.RedisCacheManager;
import com.moensun.spring.cache.interceptor.MSCacheResolver;
import com.moensun.spring.cache.interceptor.RedisCachingConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:46
 */
@Configuration
public class AbstractMSCachingConfiguration implements ImportAware {

    protected AnnotationAttributes enableCaching;

    protected RedisCacheManager cacheManager;

    protected MSCacheResolver cacheResolver;

    protected KeyGenerator keyGenerator;

    protected CacheErrorHandler errorHandler;


    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableCaching = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableMSCaching.class.getName(), false));
        if (this.enableCaching == null) {
            throw new IllegalArgumentException(
                    "@EnableMSCaching is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Autowired(required = false)
    void setConfigurers(Collection<RedisCachingConfigurer> configurers) {
        if (CollectionUtils.isEmpty(configurers)) {
            return;
        }
        if (configurers.size() > 1) {
            throw new IllegalStateException(configurers.size() + " implementations of " +
                    "CachingConfigurer were found when only 1 was expected. " +
                    "Refactor the configuration such that CachingConfigurer is " +
                    "implemented only once or not at all.");
        }
        RedisCachingConfigurer configurer = configurers.iterator().next();
        useCachingConfigurer(configurer);
    }

    /**
     * Extract the configuration from the nominated {@link RedisCachingConfigurer}.
     */
    protected void useCachingConfigurer(RedisCachingConfigurer config) {
        this.cacheManager = config.cacheManager();
        this.cacheResolver = config.cacheResolver();
        this.keyGenerator = config.keyGenerator();
        this.errorHandler = config.errorHandler();
    }

}
