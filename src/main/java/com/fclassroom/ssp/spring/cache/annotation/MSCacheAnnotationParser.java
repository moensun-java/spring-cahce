package com.fclassroom.ssp.spring.cache.annotation;

import com.fclassroom.ssp.spring.cache.operation.MSCacheOperation;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午12:54
 */
public interface MSCacheAnnotationParser {

    /**
     * Parses the cache definition for the given class,
     * based on a known annotation type.
     * <p>This essentially parses a known cache annotation into Spring's
     * metadata attribute class. Returns {@code null} if the class
     * is not cacheable.
     * @param type the annotated class
     * @return CacheOperation the configured caching operation,
     * or {@code null} if none was found
     * @see AnnotationCacheOperationSource#findCacheOperations(Class)
     */
    Collection<MSCacheOperation> parseCacheAnnotations(Class<?> type);

    /**
     * Parses the cache definition for the given method,
     * based on a known annotation type.
     * <p>This essentially parses a known cache annotation into Spring's
     * metadata attribute class. Returns {@code null} if the method
     * is not cacheable.
     * @param method the annotated method
     * @return CacheOperation the configured caching operation,
     * or {@code null} if none was found
     * @see AnnotationCacheOperationSource#findCacheOperations(Method)
     */
    Collection<MSCacheOperation> parseCacheAnnotations(Method method);

}
