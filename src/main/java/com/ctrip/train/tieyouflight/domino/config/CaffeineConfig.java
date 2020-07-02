package com.ctrip.train.tieyouflight.domino.config;

import com.ctrip.train.tieyouflight.domino.support.serialize.Serializer;

import java.time.Duration;

/**
 * @author wang.wei
 * @since 2019/6/25
 */
public class CaffeineConfig {

    private boolean cacheNullValues;
    private boolean cacheEmptyValues;
    private Duration ttl;
    private long maxSize;
    private Serializer serializer;

    public Serializer getSerializer() {
        return serializer;
    }

    public CaffeineConfig setSerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }

    public boolean isCacheEmptyValues() {
        return cacheEmptyValues;
    }

    public CaffeineConfig setCacheEmptyValues(boolean cacheEmptyValues) {
        this.cacheEmptyValues = cacheEmptyValues;
        return this;
    }

    public CaffeineConfig setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
        return this;
    }

    public Duration getTtl() {
        return ttl;
    }

    public CaffeineConfig setTtl(Duration ttl) {
        this.ttl = ttl;
        return this;
    }


    public long getMaxSize() {
        return maxSize;
    }

    public CaffeineConfig setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }
}
