package com.moensun.spring.cache.annotation;

import com.moensun.spring.cache.operation.MSCacheableOperation;
import com.moensun.spring.cache.operation.MSCacheEvictOperation;
import com.moensun.spring.cache.operation.MSCacheOperation;
import com.moensun.spring.cache.operation.MSCachePutOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 上午11:41
 */
public class MSMSCacheAnnotationParser implements MSCacheAnnotationParser,Serializable {
    @Override
    public Collection<MSCacheOperation> parseCacheAnnotations(Class<?> type) {
        DefaultCacheConfig defaultConfig = getDefaultCacheConfig(type);
        return parseCacheAnnotations(defaultConfig, type);
    }

    @Override
    public Collection<MSCacheOperation> parseCacheAnnotations(Method method) {
        DefaultCacheConfig defaultConfig = getDefaultCacheConfig(method.getDeclaringClass());
        return parseCacheAnnotations(defaultConfig, method);
    }

    protected Collection<MSCacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, AnnotatedElement ae){
        Collection<MSCacheOperation> ops = null;

        Collection<MSCacheable> cacheables = AnnotatedElementUtils.getAllMergedAnnotations(ae, MSCacheable.class);
        if (!cacheables.isEmpty()) {
            ops = lazyInit(ops);
            for (MSCacheable cacheable : cacheables) {
                ops.add(parseCacheableAnnotation(ae, cachingConfig, cacheable));
            }
        }

        Collection<MSCacheEvict> evicts = AnnotatedElementUtils.getAllMergedAnnotations(ae, MSCacheEvict.class);
        if (!evicts.isEmpty()) {
            ops = lazyInit(ops);
            for (MSCacheEvict evict : evicts) {
                ops.add(parseEvictAnnotation(ae, cachingConfig, evict));
            }
        }

        Collection<MSCachePut> puts = AnnotatedElementUtils.getAllMergedAnnotations(ae, MSCachePut.class);
        if (!puts.isEmpty()) {
            ops = lazyInit(ops);
            for (MSCachePut put : puts) {
                ops.add(parsePutAnnotation(ae, cachingConfig, put));
            }
        }

        Collection<MSCaching> cachings = AnnotatedElementUtils.getAllMergedAnnotations(ae, MSCaching.class);
        if (!cachings.isEmpty()) {
            ops = lazyInit(ops);
            for (MSCaching caching : cachings) {
                Collection<MSCacheOperation> cachingOps = parseCachingAnnotation(ae, cachingConfig, caching);
                if (cachingOps != null) {
                    ops.addAll(cachingOps);
                }
            }
        }

        return ops;
    }

    private <T extends Annotation> Collection<MSCacheOperation> lazyInit(Collection<MSCacheOperation> ops) {
        return (ops != null ? ops : new ArrayList<MSCacheOperation>(1));
    }

    MSCacheableOperation parseCacheableAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, MSCacheable cacheable) {
        MSCacheableOperation.Builder builder = new MSCacheableOperation.Builder();

        builder.setName(ae.toString());
        builder.setCacheNames(cacheable.cacheNames());
        builder.setCondition(cacheable.condition());
        builder.setUnless(cacheable.unless());
        builder.setKey(cacheable.key());
        builder.setKeyGenerator(cacheable.keyGenerator());
        builder.setCacheManager(cacheable.cacheManager());
        builder.setCacheResolver(cacheable.cacheResolver());
        builder.setSync(cacheable.sync());

        builder.setHashKey(cacheable.hashKey());
        builder.setHashKeyGenerator(cacheable.hashKeyGenerator());
        builder.setDataType(cacheable.dataType());

        defaultConfig.applyDefault(builder);
        MSCacheableOperation op = builder.build();
        validateCacheOperation(ae, op);

        return op;
    }

    MSCacheEvictOperation parseEvictAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, MSCacheEvict cacheEvict) {
        MSCacheEvictOperation.Builder builder = new MSCacheEvictOperation.Builder();

        builder.setName(ae.toString());
        builder.setCacheNames(cacheEvict.cacheNames());
        builder.setCondition(cacheEvict.condition());
        builder.setKey(cacheEvict.key());
        builder.setKeyGenerator(cacheEvict.keyGenerator());
        builder.setCacheManager(cacheEvict.cacheManager());
        builder.setCacheResolver(cacheEvict.cacheResolver());
        builder.setCacheWide(cacheEvict.allEntries());
        builder.setBeforeInvocation(cacheEvict.beforeInvocation());

        builder.setHashKey(cacheEvict.hashKey());
        builder.setHashKeyGenerator(cacheEvict.hashKeyGenerator());
        builder.setDataType(cacheEvict.dataType());

        defaultConfig.applyDefault(builder);
        MSCacheEvictOperation op = builder.build();
        validateCacheOperation(ae, op);

        return op;
    }

    MSCacheOperation parsePutAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, MSCachePut cachePut) {
        MSCachePutOperation.Builder builder = new MSCachePutOperation.Builder();

        builder.setName(ae.toString());
        builder.setCacheNames(cachePut.cacheNames());
        builder.setCondition(cachePut.condition());
        builder.setUnless(cachePut.unless());
        builder.setKey(cachePut.key());
        builder.setKeyGenerator(cachePut.keyGenerator());
        builder.setCacheManager(cachePut.cacheManager());
        builder.setCacheResolver(cachePut.cacheResolver());

        builder.setHashKey(cachePut.hashKey());
        builder.setHashKeyGenerator(cachePut.hashKeyGenerator());
        builder.setDataType(cachePut.dataType());

        defaultConfig.applyDefault(builder);
        MSCacheOperation op = builder.build();
        validateCacheOperation(ae, op);

        return op;
    }


    Collection<MSCacheOperation> parseCachingAnnotation(AnnotatedElement ae, DefaultCacheConfig defaultConfig, MSCaching caching) {
        Collection<MSCacheOperation> ops = null;

        MSCacheable[] cacheables = caching.cacheable();
        if (!ObjectUtils.isEmpty(cacheables)) {
            ops = lazyInit(ops);
            for (MSCacheable cacheable : cacheables) {
                ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
            }
        }
        MSCacheEvict[] cacheEvicts = caching.evict();
        if (!ObjectUtils.isEmpty(cacheEvicts)) {
            ops = lazyInit(ops);
            for (MSCacheEvict cacheEvict : cacheEvicts) {
                ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
            }
        }
        MSCachePut[] cachePuts = caching.put();
        if (!ObjectUtils.isEmpty(cachePuts)) {
            ops = lazyInit(ops);
            for (MSCachePut cachePut : cachePuts) {
                ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
            }
        }

        return ops;
    }


    DefaultCacheConfig getDefaultCacheConfig(Class<?> target) {
        MSCacheConfig annotation = AnnotatedElementUtils.getMergedAnnotation(target, MSCacheConfig.class);
        if (annotation != null) {
            return new DefaultCacheConfig(annotation.cacheNames(), annotation.keyGenerator(),annotation.hashKeyGenerator(),
                    annotation.cacheManager(), annotation.cacheResolver());
        }
        return new DefaultCacheConfig();
    }

    private void validateCacheOperation(AnnotatedElement ae, CacheOperation operation) {
        if (StringUtils.hasText(operation.getKey()) && StringUtils.hasText(operation.getKeyGenerator())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" +
                    ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
                    "These attributes are mutually exclusive: either set the SpEL expression used to" +
                    "compute the key at runtime or set the name of the KeyGenerator bean to use.");
        }
        if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" +
                    ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
                    "These attributes are mutually exclusive: the cache manager is used to configure a" +
                    "default cache resolver if none is set. If a cache resolver is set, the cache manager" +
                    "won't be used.");
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || other instanceof MSMSCacheAnnotationParser);
    }

    @Override
    public int hashCode() {
        return MSMSCacheAnnotationParser.class.hashCode();
    }

    static class DefaultCacheConfig {

        private final String[] cacheNames;

        private final String keyGenerator;

        private final String hashKeyGenerator;

        private final String cacheManager;

        private final String cacheResolver;

        public DefaultCacheConfig() {
            this(null, null, null,null, null);
        }

        private DefaultCacheConfig(String[] cacheNames, String keyGenerator, String hashKeyGenerator,String cacheManager, String cacheResolver) {
            this.cacheNames = cacheNames;
            this.keyGenerator = keyGenerator;
            this.hashKeyGenerator = hashKeyGenerator;
            this.cacheManager = cacheManager;
            this.cacheResolver = cacheResolver;
        }

        /**
         * Apply the defaults to the specified {@link CacheOperation.Builder}.
         * @param builder the operation builder to update
         */
        public void applyDefault(MSCacheOperation.Builder builder) {
            if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
                builder.setCacheNames(this.cacheNames);
            }
            if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
                    StringUtils.hasText(this.keyGenerator)) {
                builder.setKeyGenerator(this.keyGenerator);
            }

            if (!StringUtils.hasText(builder.getHashKey()) && !StringUtils.hasText(builder.getHashKeyGenerator()) &&
                    StringUtils.hasText(this.hashKeyGenerator)) {
                builder.setKeyGenerator(this.hashKeyGenerator);
            }

            if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
                // One of these is set so we should not inherit anything
            }
            else if (StringUtils.hasText(this.cacheResolver)) {
                builder.setCacheResolver(this.cacheResolver);
            }
            else if (StringUtils.hasText(this.cacheManager)) {
                builder.setCacheManager(this.cacheManager);
            }
        }

    }

}
