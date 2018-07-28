package com.moensun.spring.cache.operation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodClassKey;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午12:49
 */
public abstract class AbstractFallbackMSCacheOperationSource implements MSCacheOperationSource {

    /**
     * Canonical value held in cache to indicate no caching attribute was
     * found for this method and we don't need to look again.
     */
    private final static Collection<MSCacheOperation> NULL_CACHING_ATTRIBUTE = Collections.emptyList();


    /**
     * Logger available to subclasses.
     * <p>As this base class is not marked Serializable, the logger will be recreated
     * after serialization - provided that the concrete subclass is Serializable.
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Cache of CacheOperations, keyed by method on a specific target class.
     * <p>As this base class is not marked Serializable, the cache will be recreated
     * after serialization - provided that the concrete subclass is Serializable.
     */
    private final Map<Object, Collection<MSCacheOperation>> attributeCache =
            new ConcurrentHashMap<Object, Collection<MSCacheOperation>>(1024);


    /**
     * Determine the caching attribute for this method invocation.
     * <p>Defaults to the class's caching attribute if no method attribute is found.
     * @param method the method for the current invocation (never {@code null})
     * @param targetClass the target class for this invocation (may be {@code null})
     * @return {@link MSCacheOperation} for this method, or {@code null} if the method
     * is not cacheable
     */
    @Override
    public Collection<MSCacheOperation> getCacheOperations(Method method, Class<?> targetClass) {
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }

        Object cacheKey = getCacheKey(method, targetClass);
        Collection<MSCacheOperation> cached = this.attributeCache.get(cacheKey);

        if (cached != null) {
            return (cached != NULL_CACHING_ATTRIBUTE ? cached : null);
        }
        else {
            Collection<MSCacheOperation> cacheOps = computeCacheOperations(method, targetClass);
            if (cacheOps != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
                }
                this.attributeCache.put(cacheKey, cacheOps);
            }
            else {
                this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
            }
            return cacheOps;
        }
    }

    /**
     * Determine a cache key for the given method and target class.
     * <p>Must not produce same key for overloaded methods.
     * Must produce same key for different instances of the same method.
     * @param method the method (never {@code null})
     * @param targetClass the target class (may be {@code null})
     * @return the cache key (never {@code null})
     */
    protected Object getCacheKey(Method method, Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }

    private Collection<MSCacheOperation> computeCacheOperations(Method method, Class<?> targetClass) {
        // Don't allow no-public methods as required.
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        // The method may be on an interface, but we need attributes from the target class.
        // If the target class is null, the method will be unchanged.
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
        // If we are dealing with method with generic parameters, find the original method.
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

        // First try is the method in the target class.
        Collection<MSCacheOperation> opDef = findCacheOperations(specificMethod);
        if (opDef != null) {
            return opDef;
        }

        // Second try is the caching operation on the target class.
        opDef = findCacheOperations(specificMethod.getDeclaringClass());
        if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
            return opDef;
        }

        if (specificMethod != method) {
            // Fallback is to look at the original method.
            opDef = findCacheOperations(method);
            if (opDef != null) {
                return opDef;
            }
            // Last fallback is the class of the original method.
            opDef = findCacheOperations(method.getDeclaringClass());
            if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
                return opDef;
            }
        }

        return null;
    }


    /**
     * Subclasses need to implement this to return the caching attribute
     * for the given method, if any.
     * @param method the method to retrieve the attribute for
     * @return all caching attribute associated with this method
     * (or {@code null} if none)
     */
    protected abstract Collection<MSCacheOperation> findCacheOperations(Method method);

    /**
     * Subclasses need to implement this to return the caching attribute
     * for the given class, if any.
     * @param clazz the class to retrieve the attribute for
     * @return all caching attribute associated with this class
     * (or {@code null} if none)
     */
    protected abstract Collection<MSCacheOperation> findCacheOperations(Class<?> clazz);

    /**
     * Should only public methods be allowed to have caching semantics?
     * <p>The default implementation returns {@code false}.
     */
    protected boolean allowPublicMethodsOnly() {
        return false;
    }
    
}
