package com.ctrip.train.tieyouflight.domino.config;

import com.ctrip.train.tieyouflight.domino.support.KeyWrapper;
import com.ctrip.train.tieyouflight.domino.support.Serializer;

import java.time.Duration;

/**
 * @author wang.wei
 * @since 2019/6/25
 */
public class RedisConfig {
    private Duration ttl;
    private boolean cacheNullValues;
    private boolean cacheEmptyValues;
    private KeyWrapper keyWrapper;
    private Serializer serializer;

    public boolean isCacheEmptyValues() {
        return cacheEmptyValues;
    }

    public RedisConfig setCacheEmptyValues(boolean cacheEmptyValues) {
        this.cacheEmptyValues = cacheEmptyValues;
        return this;
    }

    public Duration getTtl() {
        return ttl;
    }

    public RedisConfig setTtl(Duration ttl) {
        this.ttl = ttl;
        return this;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public RedisConfig setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
        return this;
    }

    public KeyWrapper getKeyWrapper() {
        return keyWrapper;
    }

    public RedisConfig setKeyWrapper(KeyWrapper keyWrapper) {
        this.keyWrapper = keyWrapper;
        return this;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public RedisConfig setSerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }
}
