package com.moensun.spring.cache.interceptor;

import com.moensun.spring.cache.annotation.DataType;
import org.springframework.cache.support.SimpleValueWrapper;

import static org.springframework.util.Assert.notNull;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午4:04
 */
public class MSCacheElement extends SimpleValueWrapper {

    private final MSCacheKey cacheKey;
    private long timeToLive;


    public MSCacheElement(MSCacheKey cacheKey, Object value) {
        super(value);

        notNull(cacheKey, "CacheKey must not be null!");
        this.cacheKey = cacheKey;
    }

    /**
     * Get the binary key representation.
     *
     * @return
     */
    public byte[] getKeyBytes() {
        return cacheKey.getKeyBytes();
    }

    public byte[] getHashKeyBytes(){
        return cacheKey.getHashKeyBytes();
    }

    public Object getkey(){
        return cacheKey.getKeyElement();
    }

    public Object getHashKey(){
        return cacheKey.getHashKeyElement();
    }

    public DataType getDataType(){
        return cacheKey.getDataType();
    }

    /**
     * @return
     */
    public MSCacheKey getKey() {
        return cacheKey;
    }

    /**
     * Set the elements time to live. Use {@literal zero} to store eternally.
     *
     * @param timeToLive
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * @return
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * @return true in case {@link MSCacheKey} is prefixed.
     */
    public boolean hasKeyPrefix() {
        return cacheKey.hasPrefix();
    }

    /**
     * @return true if timeToLive is 0
     */
    public boolean isEternal() {
        return 0 == timeToLive;
    }

    /**
     * Expire the element after given seconds.
     *
     * @param seconds
     * @return
     */
    public MSCacheElement expireAfter(long seconds) {

        setTimeToLive(seconds);
        return this;
    }

}
