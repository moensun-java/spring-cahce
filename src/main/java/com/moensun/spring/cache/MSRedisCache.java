package com.moensun.spring.cache;

import com.moensun.spring.cache.annotation.DataType;
import com.moensun.spring.cache.interceptor.AbstractValueAdaptingMSCache;
import com.moensun.spring.cache.interceptor.MSCacheElement;
import com.moensun.spring.cache.interceptor.MSCacheKey;
import com.moensun.spring.cache.interceptor.SimpleMSValueWrapper;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheElement;
import org.springframework.data.redis.connection.DecoratedRedisConnection;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.serializer.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午6:49
 */
public class MSRedisCache extends AbstractValueAdaptingMSCache {


    @SuppressWarnings("rawtypes") //
    private final RedisOperations redisOperations;
    private final RedisCacheMetadata cacheMetadata;
    private final CacheValueAccessor cacheValueAccessor;

    /**
     * Constructs a new {@link RedisCache} instance.
     *
     * @param name cache name
     * @param prefix
     * @param redisOperations
     * @param expiration
     */
    public MSRedisCache(String name, byte[] prefix, RedisOperations<? extends Object, ? extends Object> redisOperations,
                        long expiration) {
        this(name, prefix, redisOperations, expiration, false);
    }

    /**
     * Constructs a new {@link RedisCache} instance.
     *
     * @param name cache name
     * @param prefix must not be {@literal null} or empty.
     * @param redisOperations
     * @param expiration
     * @param allowNullValues
     * @since 1.8
     */
    public MSRedisCache(String name, byte[] prefix, RedisOperations<? extends Object, ? extends Object> redisOperations,
                        long expiration, boolean allowNullValues) {

        super(allowNullValues);

        Assert.hasText(name, "CacheName must not be null or empty!");

        RedisSerializer<?> serializer = redisOperations.getValueSerializer() != null ? redisOperations.getValueSerializer()
                : (RedisSerializer<?>) new JdkSerializationRedisSerializer();

        this.cacheMetadata = new RedisCacheMetadata(name, prefix);
        this.cacheMetadata.setDefaultExpiration(expiration);
        this.redisOperations = redisOperations;
        this.cacheValueAccessor = new CacheValueAccessor(serializer);

        if (allowNullValues) {

            if (redisOperations.getValueSerializer() instanceof StringRedisSerializer
                    || redisOperations.getValueSerializer() instanceof GenericToStringSerializer
                    || redisOperations.getValueSerializer() instanceof JacksonJsonRedisSerializer
                    || redisOperations.getValueSerializer() instanceof Jackson2JsonRedisSerializer) {
                throw new IllegalArgumentException(String.format(
                        "Redis does not allow keys with null value ¯\\_(ツ)_/¯. "
                                + "The chosen %s does not support generic type handling and therefore cannot be used with allowNullValues enabled. "
                                + "Please use a different RedisSerializer or disable null value support.",
                        ClassUtils.getShortName(redisOperations.getValueSerializer().getClass())));
            }
        }
    }

    /**
     * Return the value to which this cache maps the specified key, generically specifying a type that return value will
     * be cast to.
     *
     * @param key
     * @param type
     * @return
     * @see DATAREDIS-243
     */
    public <T> T get(Object key, Class<T> type) {

        Cache.ValueWrapper wrapper = get(key);
        return wrapper == null ? null : (T) wrapper.get();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#get(java.lang.Object)
     */
    @Override
    public Cache.ValueWrapper get(Object key) {
        return get(getRedisCacheKey(key));
    }

    /*
     * @see  org.springframework.cache.Cache#get(java.lang.Object, java.util.concurrent.Callable)
     * introduced in springframework 4.3.0.RC1
     */
    public <T> T get(final Object key, final Callable<T> valueLoader) {

        MSCacheElement cacheElement = new MSCacheElement(getRedisCacheKey(key),
                new StoreTranslatingCallable(valueLoader)).expireAfter(cacheMetadata.getDefaultExpiration());
        BinaryCacheElement rce = new BinaryCacheElement(cacheElement, cacheValueAccessor);

        Cache.ValueWrapper val = get(key);
        if (val != null) {
            return (T) val.get();
        }

        RedisWriteThroughCallback callback = new RedisWriteThroughCallback(rce, cacheMetadata);

        try {
            byte[] result = (byte[]) redisOperations.execute(callback);
            return (T) (result == null ? null : fromStoreValue(cacheValueAccessor.deserializeIfNecessary(result)));
        } catch (RuntimeException e) {
            throw CacheValueRetrievalExceptionFactory.INSTANCE.create(key, valueLoader, e);
        }
    }

    /**
     * Return the value to which this cache maps the specified key.
     *
     * @param cacheKey the key whose associated value is to be returned via its binary representation.
     * @return the {@link RedisCacheElement} stored at given key or {@literal null} if no value found for key.
     * @since 1.5
     */
    public MSCacheElement get(final MSCacheKey cacheKey) {

        Assert.notNull(cacheKey, "CacheKey must not be null!");

        Boolean exists = (Boolean) redisOperations.execute(new RedisCallback<Boolean>() {

            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.exists(cacheKey.getKeyBytes());
            }
        });

        if (!exists.booleanValue()) {
            return null;
        }

        return new MSCacheElement(cacheKey, fromStoreValue(lookup(cacheKey)));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public void put(final Object key, final Object value) {

        put(new MSCacheElement(getRedisCacheKey(key), toStoreValue(value))
                .expireAfter(cacheMetadata.getDefaultExpiration()));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.support.AbstractValueAdaptingCache#fromStoreValue(java.lang.Object)
     */
    @Override
    protected Object fromStoreValue(Object storeValue) {

        // we need this override for the GenericJackson2JsonRedisSerializer support.
        if (isAllowNullValues() && storeValue instanceof NullValue) {
            return null;
        }

        return super.fromStoreValue(storeValue);
    }

    /**
     * Add the element by adding {@link RedisCacheElement#get()} at {@link RedisCacheElement#getKeyBytes()}. If the cache
     * previously contained a mapping for this {@link RedisCacheElement#getKeyBytes()}, the old value is replaced by
     * {@link RedisCacheElement#get()}.
     *
     * @param element must not be {@literal null}.
     * @since 1.5
     */
    public void put(MSCacheElement element) {

        Assert.notNull(element, "Element must not be null!");

        redisOperations
                .execute(new RedisCachePutCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata));
    }


    //region Hash 结构
    public void hSet(MSCacheElement element){
        Assert.notNull(element, "Element must not be null!");
        redisOperations
                .execute(new RedisCacheHSetCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata));
    }
    //endregion



    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public Cache.ValueWrapper putIfAbsent(Object key, final Object value) {

        return putIfAbsent(new MSCacheElement(getRedisCacheKey(key), toStoreValue(value))
                .expireAfter(cacheMetadata.getDefaultExpiration()));
    }

    /**
     * Add the element as long as no element exists at {@link RedisCacheElement#getKeyBytes()}. If a value is present for
     * {@link RedisCacheElement#getKeyBytes()} this one is returned.
     *
     * @param element must not be {@literal null}.
     * @return
     * @since 1.5
     */
    public Cache.ValueWrapper putIfAbsent(MSCacheElement element) {

        Assert.notNull(element, "Element must not be null!");

        new RedisCachePutIfAbsentCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata);

        return toWrapper(cacheValueAccessor.deserializeIfNecessary((byte[]) redisOperations.execute(
                new RedisCachePutIfAbsentCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata))));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#evict(java.lang.Object)
     */
    public void evict(Object key) {
        evict(new MSCacheElement(getRedisCacheKey(key), null));
    }

    /**
     * @param element {@link RedisCacheElement#getKeyBytes()}
     * @since 1.5
     */
    public void evict(final MSCacheElement element) {

        Assert.notNull(element, "Element must not be null!");
        redisOperations
                .execute(new RedisCacheEvictCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#clear()
     */
    public void clear() {
        redisOperations.execute(cacheMetadata.usesKeyPrefix() ? new RedisCacheCleanByPrefixCallback(cacheMetadata)
                : new RedisCacheCleanByKeysCallback(cacheMetadata));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.Cache#getName()
     */
    public String getName() {
        return cacheMetadata.getCacheName();
    }

    /**
     * {@inheritDoc} This implementation simply returns the RedisTemplate used for configuring the cache, giving access to
     * the underlying Redis store.
     */
    public Object getNativeCache() {
        return redisOperations;
    }

    private Cache.ValueWrapper toWrapper(Object value) {
        return (value != null ? new SimpleMSValueWrapper(value) : null);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.cache.support.AbstractValueAdaptingCache#lookup(java.lang.Object)
     */
    @Override
    protected Object lookup(Object key) {

        MSCacheKey cacheKey = key instanceof MSCacheKey ? (MSCacheKey) key : getRedisCacheKey(key);

        byte[] bytes = (byte[]) redisOperations.execute(new AbstractRedisCacheCallback<byte[]>(
                new BinaryCacheElement(new MSCacheElement(cacheKey, null), cacheValueAccessor), cacheMetadata) {

            @Override
            public byte[] doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {
                return connection.get(element.getKeyBytes());
            }
        });

        return bytes == null ? null : cacheValueAccessor.deserializeIfNecessary(bytes);
    }

    @Override
    protected Object hLookup(Object key) {
        MSCacheKey cacheKey = key instanceof MSCacheKey ? (MSCacheKey) key : getRedisCacheKey(key);

        byte[] bytes = (byte[]) redisOperations.execute(new AbstractRedisCacheCallback<byte[]>(
                new BinaryCacheElement(new MSCacheElement(cacheKey, null), cacheValueAccessor), cacheMetadata) {

            @Override
            public byte[] doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {
                return connection.hGet(element.getKeyBytes(),element.getHashKeyBytes());
            }
        });

        return bytes == null ? null : cacheValueAccessor.deserializeIfNecessary(bytes);
    }

    private MSCacheKey getRedisCacheKey(Object key) {
        return new MSCacheKey(key).usePrefix(this.cacheMetadata.getKeyPrefix()).withKeySerializer(redisOperations.getKeySerializer());
    }

    private MSCacheKey getRedisCacheKey(Object key, DataType dataType, Object hashKey) {
        return new MSCacheKey(key,dataType,hashKey).usePrefix(this.cacheMetadata.getKeyPrefix()).withKeySerializer(redisOperations.getKeySerializer());
    }


    @Override
    public <T> T hGet(Object key, Object hashKey, Class<T> type) {
        return null;
    }

    @Override
    public <T> T hGet(Object key, Object hashKey, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void hSet(Object key, Object hashKey, Object value) {
        hSet(new MSCacheElement(getRedisCacheKey(key,DataType.HASH,hashKey), toStoreValue(value)).expireAfter(cacheMetadata.getDefaultExpiration()));
    }

    @Override
    public Cache.ValueWrapper hGet(Object key, Object hashKey) {
        MSCacheKey redisCacheKey = getRedisCacheKey(key,DataType.HASH,hashKey);
        return hGet(redisCacheKey);
    }

    public MSCacheElement hGet(final MSCacheKey cacheKey) {

        Assert.notNull(cacheKey, "CacheKey must not be null!");

        Boolean exists = (Boolean) redisOperations.execute(new RedisCallback<Boolean>() {

            @Override
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.hExists(cacheKey.getKeyBytes(),cacheKey.getHashKeyBytes());
            }
        });

        if (!exists.booleanValue()) {
            return null;
        }

        return new MSCacheElement(cacheKey, fromStoreValue(hLookup(cacheKey)));
    }

    @Override
    public void hEvict(Object key, Object hashKey) {
        hEvict(new MSCacheElement(getRedisCacheKey(key,DataType.HASH,hashKey), null));
    }

    public void hEvict(final MSCacheElement element) {

        Assert.notNull(element, "Element must not be null!");
        redisOperations
                .execute(new RedisCacheHEvictCallback(new BinaryCacheElement(element, cacheValueAccessor), cacheMetadata));
    }


    /**
     * {@link Callable} to transform a value obtained from another {@link Callable} to its store value.
     *
     * @author Mark Paluch
     * @since 1.8
     * @see #toStoreValue(Object)
     */
    private class StoreTranslatingCallable implements Callable<Object> {

        private Callable<?> valueLoader;

        public StoreTranslatingCallable(Callable<?> valueLoader) {
            this.valueLoader = valueLoader;
        }

        @Override
        public Object call() throws Exception {
            return toStoreValue(valueLoader.call());
        }
    }

    /**
     * Metadata required to maintain {@link RedisCache}. Keeps track of additional data structures required for processing
     * cache operations.
     *
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCacheMetadata {

        private final String cacheName;
        private final byte[] keyPrefix;
        private final byte[] setOfKnownKeys;
        private final byte[] cacheLockName;
        private long defaultExpiration = 0;

        /**
         * @param cacheName must not be {@literal null} or empty.
         * @param keyPrefix can be {@literal null}.
         */
        public RedisCacheMetadata(String cacheName, byte[] keyPrefix) {

            Assert.hasText(cacheName, "CacheName must not be null or empty!");
            this.cacheName = cacheName;
            this.keyPrefix = keyPrefix;

            StringRedisSerializer stringSerializer = new StringRedisSerializer();

            // name of the set holding the keys
            this.setOfKnownKeys = usesKeyPrefix() ? new byte[] {} : stringSerializer.serialize(cacheName + "~keys");
            this.cacheLockName = stringSerializer.serialize(cacheName + "~lock");
        }

        /**
         * @return true if the {@link RedisCache} uses a prefix for key ranges.
         */
        public boolean usesKeyPrefix() {
            return (keyPrefix != null && keyPrefix.length > 0);
        }

        /**
         * Get the binary representation of the key prefix.
         *
         * @return never {@literal null}.
         */
        public byte[] getKeyPrefix() {
            return this.keyPrefix;
        }

        /**
         * Get the binary representation of the key identifying the data structure used to maintain known keys.
         *
         * @return never {@literal null}.
         */
        public byte[] getSetOfKnownKeysKey() {
            return setOfKnownKeys;
        }

        /**
         * Get the binary representation of the key identifying the data structure used to lock the cache.
         *
         * @return never {@literal null}.
         */
        public byte[] getCacheLockKey() {
            return cacheLockName;
        }

        /**
         * Get the name of the cache.
         *
         * @return
         */
        public String getCacheName() {
            return cacheName;
        }

        /**
         * Set the default expiration time in seconds
         *
         * @param seconds
         */
        public void setDefaultExpiration(long seconds) {
            this.defaultExpiration = seconds;
        }

        /**
         * Get the default expiration time in seconds.
         *
         * @return
         */
        public long getDefaultExpiration() {
            return defaultExpiration;
        }

    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class CacheValueAccessor {

        @SuppressWarnings("rawtypes") //
        private final RedisSerializer valueSerializer;

        @SuppressWarnings("rawtypes")
        CacheValueAccessor(RedisSerializer valueRedisSerializer) {
            valueSerializer = valueRedisSerializer;
        }

        byte[] convertToBytesIfNecessary(Object value) {

            if (value == null) {
                return new byte[0];
            }

            if (valueSerializer == null && value instanceof byte[]) {
                return (byte[]) value;
            }

            return valueSerializer.serialize(value);
        }

        Object deserializeIfNecessary(byte[] value) {

            if (valueSerializer != null) {
                return valueSerializer.deserialize(value);
            }

            return value;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.6
     */
    public static class BinaryCacheElement extends MSCacheElement {

        private byte[] keyBytes;
        private byte[] valueBytes;
        private MSCacheElement element;
        private boolean lazyLoad;
        private CacheValueAccessor accessor;

        private DataType dataType;
        private byte[] hashKeyBytes;

        public BinaryCacheElement(MSCacheElement element, CacheValueAccessor accessor) {

            super(element.getKey(), element.get());
            this.element = element;
            this.keyBytes = element.getKeyBytes();
            this.accessor = accessor;

            lazyLoad = element.get() instanceof Callable;
            this.valueBytes = lazyLoad ? null : accessor.convertToBytesIfNecessary(element.get());
        }

        @Override
        public byte[] getKeyBytes() {
            return keyBytes;
        }

        public long getTimeToLive() {
            return element.getTimeToLive();
        }

        public boolean hasKeyPrefix() {
            return element.hasKeyPrefix();
        }

        public boolean isEternal() {
            return element.isEternal();
        }

        public DataType getDataType() {
            return element.getDataType();
        }

        public byte[] getHashKeyBytes() {
            return element.getHashKeyBytes();
        }

        public MSCacheElement expireAfter(long seconds) {
            return element.expireAfter(seconds);
        }

        @Override
        public byte[] get() {

            if (lazyLoad && valueBytes == null) {
                try {
                    valueBytes = accessor.convertToBytesIfNecessary(((Callable<?>) element.get()).call());
                } catch (Exception e) {
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e.getMessage(), e);
                }
            }
            return valueBytes;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     * @param <T>
     */
     static abstract class AbstractRedisCacheCallback<T> implements RedisCallback<T> {

        private long WAIT_FOR_LOCK_TIMEOUT = 300;
        private final BinaryCacheElement element;
        private final RedisCacheMetadata cacheMetadata;

        public AbstractRedisCacheCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            this.element = element;
            this.cacheMetadata = metadata;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.core.RedisCallback#doInRedis(org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public T doInRedis(RedisConnection connection) throws DataAccessException {
            waitForLock(connection);
            return doInRedis(element, connection);
        }

        public abstract T doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException;

        protected void processKeyExpiration(MSCacheElement element, RedisConnection connection) {
            if (!element.isEternal()) {
                connection.expire(element.getKeyBytes(), element.getTimeToLive());
            }
        }

        protected void maintainKnownKeys(MSCacheElement element, RedisConnection connection) {

            if (!element.hasKeyPrefix()) {

                connection.zAdd(cacheMetadata.getSetOfKnownKeysKey(), 0, element.getKeyBytes());

                if (!element.isEternal()) {
                    connection.expire(cacheMetadata.getSetOfKnownKeysKey(), element.getTimeToLive());
                }
            }
        }

        protected void cleanKnownKeys(MSCacheElement element, RedisConnection connection) {

            if (!element.hasKeyPrefix()) {
                connection.zRem(cacheMetadata.getSetOfKnownKeysKey(), element.getKeyBytes());
            }
        }

        protected boolean waitForLock(RedisConnection connection) {

            boolean retry;
            boolean foundLock = false;
            do {
                retry = false;
                if (connection.exists(cacheMetadata.getCacheLockKey())) {
                    foundLock = true;
                    try {
                        Thread.sleep(WAIT_FOR_LOCK_TIMEOUT);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    retry = true;
                }
            } while (retry);

            return foundLock;
        }

        protected void lock(RedisConnection connection) {
            waitForLock(connection);
            connection.set(cacheMetadata.getCacheLockKey(), "locked".getBytes());
        }

        protected void unlock(RedisConnection connection) {
            connection.del(cacheMetadata.getCacheLockKey());
        }
    }

    /**
     * @author Christoph Strobl
     * @param <T>
     * @since 1.5
     */
    static abstract class LockingRedisCacheCallback<T> implements RedisCallback<T> {

        private final RedisCacheMetadata metadata;

        public LockingRedisCacheCallback(RedisCacheMetadata metadata) {
            this.metadata = metadata;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.core.RedisCallback#doInRedis(org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public T doInRedis(RedisConnection connection) throws DataAccessException {

            if (connection.exists(metadata.getCacheLockKey())) {
                return null;
            }
            try {
                connection.set(metadata.getCacheLockKey(), metadata.getCacheLockKey());
                return doInLock(connection);
            } finally {
                connection.del(metadata.getCacheLockKey());
            }
        }

        public abstract T doInLock(RedisConnection connection);
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCacheCleanByKeysCallback extends LockingRedisCacheCallback<Void> {

        private static final int PAGE_SIZE = 128;
        private final RedisCacheMetadata metadata;

        RedisCacheCleanByKeysCallback(RedisCacheMetadata metadata) {
            super(metadata);
            this.metadata = metadata;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.LockingRedisCacheCallback#doInLock(org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public Void doInLock(RedisConnection connection) {

            int offset = 0;
            boolean finished = false;

            do {
                // need to paginate the keys
                Set<byte[]> keys = connection.zRange(metadata.getSetOfKnownKeysKey(), (offset) * PAGE_SIZE,
                        (offset + 1) * PAGE_SIZE - 1);
                finished = keys.size() < PAGE_SIZE;
                offset++;
                if (!keys.isEmpty()) {
                    connection.del(keys.toArray(new byte[keys.size()][]));
                }
            } while (!finished);

            connection.del(metadata.getSetOfKnownKeysKey());
            return null;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCacheCleanByPrefixCallback extends LockingRedisCacheCallback<Void> {

        private static final byte[] REMOVE_KEYS_BY_PATTERN_LUA = new StringRedisSerializer().serialize(
                "local keys = redis.call('KEYS', ARGV[1]); local keysCount = table.getn(keys); if(keysCount > 0) then for _, key in ipairs(keys) do redis.call('del', key); end; end; return keysCount;");
        private static final byte[] WILD_CARD = new StringRedisSerializer().serialize("*");
        private final RedisCacheMetadata metadata;

        public RedisCacheCleanByPrefixCallback(RedisCacheMetadata metadata) {
            super(metadata);
            this.metadata = metadata;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.LockingRedisCacheCallback#doInLock(org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public Void doInLock(RedisConnection connection) throws DataAccessException {

            byte[] prefixToUse = Arrays.copyOf(metadata.getKeyPrefix(), metadata.getKeyPrefix().length + WILD_CARD.length);
            System.arraycopy(WILD_CARD, 0, prefixToUse, metadata.getKeyPrefix().length, WILD_CARD.length);

            if (isClusterConnection(connection)) {

                // load keys to the client because currently Redis Cluster connections do not allow eval of lua scripts.
                Set<byte[]> keys = connection.keys(prefixToUse);
                if (!keys.isEmpty()) {
                    connection.del(keys.toArray(new byte[keys.size()][]));
                }
            } else {
                connection.eval(REMOVE_KEYS_BY_PATTERN_LUA, ReturnType.INTEGER, 0, prefixToUse);
            }

            return null;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCacheEvictCallback extends AbstractRedisCacheCallback<Void> {

        public RedisCacheEvictCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            super(element, metadata);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.AbstractRedisCacheCallback#doInRedis(org.springframework.data.redis.cache.RedisCacheElement, org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public Void doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            connection.del(element.getKeyBytes());
            cleanKnownKeys(element, connection);
            return null;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCachePutCallback extends AbstractRedisCacheCallback<Void> {

        public RedisCachePutCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {

            super(element, metadata);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.AbstractRedisPutCallback#doInRedis(org.springframework.data.redis.cache.RedisCache.RedisCacheElement, org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public Void doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            if (!isClusterConnection(connection)) {
                connection.multi();
            }

            if (element.get().length == 0) {
                connection.del(element.getKeyBytes());
            } else {
                connection.set(element.getKeyBytes(), element.get());
                processKeyExpiration(element, connection);
                maintainKnownKeys(element, connection);
            }

            if (!isClusterConnection(connection)) {
                connection.exec();
            }
            return null;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.5
     */
    static class RedisCachePutIfAbsentCallback extends AbstractRedisCacheCallback<byte[]> {

        public RedisCachePutIfAbsentCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            super(element, metadata);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.AbstractRedisPutCallback#doInRedis(org.springframework.data.redis.cache.RedisCache.RedisCacheElement, org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public byte[] doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            waitForLock(connection);

            byte[] keyBytes = element.getKeyBytes();
            byte[] value = element.get();

            if (!connection.setNX(keyBytes, value)) {
                return connection.get(keyBytes);
            }

            maintainKnownKeys(element, connection);
            processKeyExpiration(element, connection);

            return null;
        }
    }

    /**
     * @author Christoph Strobl
     * @since 1.7
     */
    static class RedisWriteThroughCallback extends AbstractRedisCacheCallback<byte[]> {

        public RedisWriteThroughCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            super(element, metadata);
        }

        @Override
        public byte[] doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            try {

                lock(connection);

                try {

                    byte[] value = connection.get(element.getKeyBytes());

                    if (value != null) {
                        return value;
                    }

                    if (!isClusterConnection(connection)) {

                        connection.watch(element.getKeyBytes());
                        connection.multi();
                    }

                    value = element.get();

                    if (value.length == 0) {
                        connection.del(element.getKeyBytes());
                    } else {
                        connection.set(element.getKeyBytes(), value);
                        processKeyExpiration(element, connection);
                        maintainKnownKeys(element, connection);
                    }

                    if (!isClusterConnection(connection)) {
                        connection.exec();
                    }

                    return value;
                } catch (RuntimeException e) {
                    if (!isClusterConnection(connection)) {
                        connection.discard();
                    }
                    throw e;
                }
            } finally {
                unlock(connection);
            }
        }
    };

    /**
     * @author Christoph Strobl
     * @since 1.7 (TODO: remove when upgrading to spring 4.3)
     */
    private static enum CacheValueRetrievalExceptionFactory {

        INSTANCE;

        private static boolean isSpring43;

        static {
            isSpring43 = ClassUtils.isPresent("org.springframework.cache.Cache$ValueRetrievalException",
                    ClassUtils.getDefaultClassLoader());
        }

        public RuntimeException create(Object key, Callable<?> valueLoader, Throwable cause) {

            if (isSpring43) {
                try {
                    Class<?> execption = ClassUtils.forName("org.springframework.cache.Cache$ValueRetrievalException",
                            this.getClass().getClassLoader());
                    Constructor<?> c = ClassUtils.getConstructorIfAvailable(execption, Object.class, Callable.class,
                            Throwable.class);
                    return (RuntimeException) c.newInstance(key, valueLoader, cause);
                } catch (Exception ex) {
                    // ignore
                }
            }

            return new RedisSystemException(
                    String.format("Value for key '%s' could not be loaded using '%s'.", key, valueLoader), cause);
        }
    }

    private static boolean isClusterConnection(RedisConnection connection) {

        while (connection instanceof DecoratedRedisConnection) {
            connection = ((DecoratedRedisConnection) connection).getDelegate();
        }

        return connection instanceof RedisClusterConnection;
    }


    //region HASH 数据结构


    static class RedisCacheHSetCallback extends AbstractRedisCacheCallback<Void>{
        public RedisCacheHSetCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            super(element, metadata);
        }

        @Override
        public Void doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            if (!isClusterConnection(connection)) {
                connection.multi();
            }

            if (element.get().length == 0) {
                connection.del(element.getKeyBytes());
            } else {
                connection.hSet(element.getKeyBytes(),element.getHashKeyBytes(), element.get());
                processKeyExpiration(element, connection);
                maintainKnownKeys(element, connection);
            }

            if (!isClusterConnection(connection)) {
                connection.exec();
            }
            return null;
        }
    }

    static class RedisCacheHEvictCallback extends AbstractRedisCacheCallback<Void> {

        public RedisCacheHEvictCallback(BinaryCacheElement element, RedisCacheMetadata metadata) {
            super(element, metadata);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.redis.cache.RedisCache.AbstractRedisCacheCallback#doInRedis(org.springframework.data.redis.cache.RedisCacheElement, org.springframework.data.redis.connection.RedisConnection)
         */
        @Override
        public Void doInRedis(BinaryCacheElement element, RedisConnection connection) throws DataAccessException {

            connection.hDel(element.getKeyBytes(),element.getHashKeyBytes());
            cleanKnownKeys(element, connection);
            return null;
        }
    }


    //endregion


}
