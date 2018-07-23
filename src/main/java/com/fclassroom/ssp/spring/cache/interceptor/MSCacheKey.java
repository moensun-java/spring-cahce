package com.fclassroom.ssp.spring.cache.interceptor;

import com.fclassroom.ssp.spring.cache.annotation.DataType;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.Arrays;

import static org.springframework.util.Assert.notNull;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/19
 * Time: 下午3:50
 */
public class MSCacheKey {

    private final Object keyElement;
    private byte[] prefix;
    @SuppressWarnings("rawtypes")//
    private RedisSerializer serializer;

    private final DataType dataType;

    private final Object hashKeyElement;


    public MSCacheKey(Object keyElement) {

        notNull(keyElement, "KeyElement must not be null!");
        this.keyElement = keyElement;
        this.dataType = DataType.STRING;
        this.hashKeyElement = null;
    }


    public MSCacheKey(Object keyElement, DataType dataType, Object hashKeyElement) {
        this.keyElement = keyElement;
        this.dataType = dataType;
        this.hashKeyElement = hashKeyElement;
    }

    /**
     * Get the {@link Byte} representation of the given key element using prefix if available.
     */
    public byte[] getKeyBytes() {

        byte[] rawKey = serializeKeyElement();
        if (!hasPrefix()) {
            return rawKey;
        }

        byte[] prefixedKey = Arrays.copyOf(prefix, prefix.length + rawKey.length);
        System.arraycopy(rawKey, 0, prefixedKey, prefix.length, rawKey.length);

        return prefixedKey;
    }

    public byte[] getHashKeyBytes() {
        return serializeHashKeyElement();
    }

    /**
     * @return
     */
    public Object getKeyElement() {
        return keyElement;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Object getHashKeyElement() {
        return hashKeyElement;
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeKeyElement() {

        if (serializer == null && keyElement instanceof byte[]) {
            return (byte[]) keyElement;
        }

        return serializer.serialize(keyElement);
    }

    @SuppressWarnings("unchecked")
    private byte[] serializeHashKeyElement() {

        if (serializer == null && hashKeyElement instanceof byte[]) {
            return (byte[]) hashKeyElement;
        }

        return serializer.serialize(hashKeyElement.toString());
    }

    /**
     * Set the {@link RedisSerializer} used for converting the key into its {@link Byte} representation.
     *
     * @param serializer can be {@literal null}.
     */
    public void setSerializer(RedisSerializer<?> serializer) {
        this.serializer = serializer;
    }

    /**
     * @return true if prefix is not empty.
     */
    public boolean hasPrefix() {
        return (prefix != null && prefix.length > 0);
    }

    /**
     * Use the given prefix when generating key.
     *
     * @param prefix can be {@literal null}.
     * @return
     */
    public MSCacheKey usePrefix(byte[] prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Use {@link RedisSerializer} for converting the key into its {@link Byte} representation.
     *
     * @param serializer can be {@literal null}.
     * @return
     */
    public MSCacheKey withKeySerializer(RedisSerializer serializer) {

        this.serializer = serializer;
        return this;
    }

}
