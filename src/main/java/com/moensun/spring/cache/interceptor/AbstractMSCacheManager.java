package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.MSCache;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午2:26
 */
public abstract class AbstractMSCacheManager implements RedisCacheManager, InitializingBean {

    private final ConcurrentMap<String, MSCache> cacheMap = new ConcurrentHashMap<String, MSCache>(16);

    private volatile Set<String> cacheNames = Collections.emptySet();


    // Early cache initialization on startup

    @Override
    public void afterPropertiesSet() {
        initializeCaches();
    }

    /**
     * Initialize the static configuration of caches.
     * <p>Triggered on startup through {@link #afterPropertiesSet()};
     * can also be called to re-initialize at runtime.
     * @since 4.2.2
     * @see #loadCaches()
     */
    public void initializeCaches() {
        Collection<? extends MSCache> caches = loadCaches();

        synchronized (this.cacheMap) {
            this.cacheNames = Collections.emptySet();
            this.cacheMap.clear();
            Set<String> cacheNames = new LinkedHashSet<String>(caches.size());
            for (MSCache cache : caches) {
                String name = cache.getName();
                this.cacheMap.put(name, decorateCache(cache));
                cacheNames.add(name);
            }
            this.cacheNames = Collections.unmodifiableSet(cacheNames);
        }
    }

    /**
     * Load the initial caches for this cache manager.
     * <p>Called by {@link #afterPropertiesSet()} on startup.
     * The returned collection may be empty but must not be {@code null}.
     */
    protected abstract Collection<? extends MSCache> loadCaches();


    // Lazy cache initialization on access

    @Override
    public MSCache getCache(String name) {
        MSCache cache = this.cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        else {
            // Fully synchronize now for missing cache creation...
            synchronized (this.cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    cache = getMissingCache(name);
                    if (cache != null) {
                        cache = decorateCache(cache);
                        this.cacheMap.put(name, cache);
                        updateCacheNames(name);
                    }
                }
                return cache;
            }
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }


    // Common cache initialization delegates for subclasses

    /**
     * Check for a registered cache of the given name.
     * In contrast to {@link #getCache(String)}, this method does not trigger
     * the lazy creation of missing caches via {@link #getMissingCache(String)}.
     * @param name the cache identifier (must not be {@code null})
     * @return the associated Cache instance, or {@code null} if none found
     * @since 4.1
     * @see #getCache(String)
     * @see #getMissingCache(String)
     */
    protected final MSCache lookupCache(String name) {
        return this.cacheMap.get(name);
    }

    /**
     * Dynamically register an additional Cache with this manager.
     * @param cache the Cache to register
     * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String)}
     */
    @Deprecated
    protected final void addCache(MSCache cache) {
        String name = cache.getName();
        synchronized (this.cacheMap) {
            if (this.cacheMap.put(name, decorateCache(cache)) == null) {
                updateCacheNames(name);
            }
        }
    }

    /**
     * Update the exposed {@link #cacheNames} set with the given name.
     * <p>This will always be called within a full {@link #cacheMap} lock
     * and effectively behaves like a {@code CopyOnWriteArraySet} with
     * preserved order but exposed as an unmodifiable reference.
     * @param name the name of the cache to be added
     */
    private void updateCacheNames(String name) {
        Set<String> cacheNames = new LinkedHashSet<String>(this.cacheNames.size() + 1);
        cacheNames.addAll(this.cacheNames);
        cacheNames.add(name);
        this.cacheNames = Collections.unmodifiableSet(cacheNames);
    }


    // Overridable template methods for cache initialization

    /**
     * Decorate the given Cache object if necessary.
     * @param cache the Cache object to be added to this CacheManager
     * @return the decorated Cache object to be used instead,
     * or simply the passed-in Cache object by default
     */
    protected MSCache decorateCache(MSCache cache) {
        return cache;
    }

    /**
     * Return a missing cache with the specified {@code name} or {@code null} if
     * such cache does not exist or could not be created on the fly.
     * <p>Some caches may be created at runtime if the native provider supports
     * it. If a lookup by name does not yield any result, a subclass gets a chance
     * to register such a cache at runtime. The returned cache will be automatically
     * added to this instance.
     * @param name the name of the cache to retrieve
     * @return the missing cache or {@code null} if no such cache exists or could be
     * created
     * @since 4.1
     * @see #getCache(String)
     */
    protected MSCache getMissingCache(String name) {
        return null;
    }

}
