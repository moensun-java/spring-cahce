package com.moensun.spring.cache.operation;

import com.moensun.spring.cache.annotation.MSMSCacheAnnotationParser;
import com.moensun.spring.cache.annotation.MSCacheAnnotationParser;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午2:57
 */
public class AnnotationMSCacheOperationSource extends AbstractFallbackMSCacheOperationSource implements Serializable {

    private final boolean publicMethodsOnly;

    private final Set<MSCacheAnnotationParser> annotationParsers;


    /**
     * Create a default AnnotationCacheOperationSource, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     */
    public AnnotationMSCacheOperationSource() {
        this(true);
    }

    /**
     * Create a default {@code AnnotationCacheOperationSource}, supporting public methods
     * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
     * @param publicMethodsOnly whether to support only annotated public methods
     * typically for use with proxy-based AOP), or protected/private methods as well
     * (typically used with AspectJ class weaving)
     */
    public AnnotationMSCacheOperationSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = new LinkedHashSet<MSCacheAnnotationParser>(1);
        this.annotationParsers.add(new MSMSCacheAnnotationParser());
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParser the CacheAnnotationParser to use
     */
    public AnnotationMSCacheOperationSource(MSCacheAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public AnnotationMSCacheOperationSource(MSCacheAnnotationParser... annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        Set<MSCacheAnnotationParser> parsers = new LinkedHashSet<MSCacheAnnotationParser>(annotationParsers.length);
        Collections.addAll(parsers, annotationParsers);
        this.annotationParsers = parsers;
    }

    /**
     * Create a custom AnnotationCacheOperationSource.
     * @param annotationParsers the CacheAnnotationParser to use
     */
    public AnnotationMSCacheOperationSource(Set<MSCacheAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }


    @Override
    protected Collection<MSCacheOperation> findCacheOperations(final Class<?> clazz) {
        return determineCacheOperations(new AnnotationMSCacheOperationSource.CacheOperationProvider() {
            @Override
            public Collection<MSCacheOperation> getCacheOperations(MSCacheAnnotationParser parser) {
                return parser.parseCacheAnnotations(clazz);
            }
        });

    }

    @Override
    protected Collection<MSCacheOperation> findCacheOperations(final Method method) {
        return determineCacheOperations(new AnnotationMSCacheOperationSource.CacheOperationProvider() {
            @Override
            public Collection<MSCacheOperation> getCacheOperations(MSCacheAnnotationParser parser) {
                return parser.parseCacheAnnotations(method);
            }
        });
    }

    /**
     * Determine the cache operation(s) for the given {@link AnnotationMSCacheOperationSource.CacheOperationProvider}.
     * <p>This implementation delegates to configured
     * {@link CacheAnnotationParser}s for parsing known annotations into
     * Spring's metadata attribute class.
     * <p>Can be overridden to support custom annotations that carry
     * caching metadata.
     * @param provider the cache operation provider to use
     * @return the configured caching operations, or {@code null} if none found
     */
    protected Collection<MSCacheOperation> determineCacheOperations(AnnotationMSCacheOperationSource.CacheOperationProvider provider) {
        Collection<MSCacheOperation> ops = null;
        for (MSCacheAnnotationParser annotationParser : this.annotationParsers) {
            Collection<MSCacheOperation> annOps = provider.getCacheOperations(annotationParser);
            if (annOps != null) {
                if (ops == null) {
                    ops = new ArrayList<MSCacheOperation>();
                }
                ops.addAll(annOps);
            }
        }
        return ops;
    }

    /**
     * By default, only public methods can be made cacheable.
     */
    @Override
    protected boolean allowPublicMethodsOnly() {
        return this.publicMethodsOnly;
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationMSCacheOperationSource)) {
            return false;
        }
        AnnotationMSCacheOperationSource otherCos = (AnnotationMSCacheOperationSource) other;
        return (this.annotationParsers.equals(otherCos.annotationParsers) &&
                this.publicMethodsOnly == otherCos.publicMethodsOnly);
    }

    @Override
    public int hashCode() {
        return this.annotationParsers.hashCode();
    }


    /**
     * Callback interface providing {@link CacheOperation} instance(s) based on
     * a given {@link CacheAnnotationParser}.
     */
    protected interface CacheOperationProvider {

        /**
         * Return the {@link CacheOperation} instance(s) provided by the specified parser.
         * @param parser the parser to use
         * @return the cache operations, or {@code null} if none found
         */
        Collection<MSCacheOperation> getCacheOperations(MSCacheAnnotationParser parser);
    }

}
