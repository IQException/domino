package com.ctrip.train.tieyouflight.domino.config;

import com.ctrip.train.tieyouflight.domino.TieredCacheManager;
import com.ctrip.train.tieyouflight.domino.support.serialize.KeyWrapper;
import com.ctrip.train.tieyouflight.domino.support.serialize.Serializer;

import java.time.Duration;

/**
 * @author wang.wei
 * @see Domino
 * @since 2019/6/26
 */
public class CacheConfig {

    private boolean autoLoad;

    private boolean useLocalCache;

    private TieredCacheManager localCacheManager;

    private boolean useRemoteCache;

    private TieredCacheManager remoteCacheManager;

    private Serializer serializer;

    private boolean refreshable;

    private Duration interval;

    private Duration refreshTimeout;

    private int retries;

    private boolean cacheNullValues;

    private boolean cacheEmptyValues;

    private Duration localExpireTime;

    private Duration remoteExpireTime;

    private int concurrency;

    private long localSize;

    private Duration warnDelayTime;

    private KeyWrapper keyWrapper;

    private String cond;

    private String except;

    public int getRetries() {
        return retries;
    }

    public boolean isCacheEmptyValues() {
        return cacheEmptyValues;
    }

    public String getCond() {
        return cond;
    }

    public CacheConfig setCond(String cond) {
        this.cond = cond;
        return this;
    }

    public String getExcept() {
        return except;
    }

    public CacheConfig setExcept(String except) {
        this.except = except;
        return this;
    }

    public CacheConfig setCacheEmptyValues(boolean cacheEmptyValues) {
        this.cacheEmptyValues = cacheEmptyValues;
        return this;
    }

    public CacheConfig setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public CacheConfig setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public long getLocalSize() {
        return localSize;
    }

    public CacheConfig setLocalSize(long localSize) {
        this.localSize = localSize;
        return this;
    }

    public CacheConfig setRemoteExpireTime(Duration remoteExpireTime) {
        this.remoteExpireTime = remoteExpireTime;
        return this;
    }

    public KeyWrapper getKeyWrapper() {
        return keyWrapper;
    }

    public CacheConfig setKeyWrapper(KeyWrapper keyWrapper) {
        this.keyWrapper = keyWrapper;
        return this;
    }

    public boolean isAutoLoad() {
        return autoLoad;
    }

    public CacheConfig setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
        return this;
    }

    public boolean isUseLocalCache() {
        return useLocalCache;
    }

    public CacheConfig setUseLocalCache(boolean useLocalCache) {
        this.useLocalCache = useLocalCache;
        return this;
    }

    public TieredCacheManager getLocalCacheManager() {
        return localCacheManager;
    }

    public CacheConfig setLocalCacheManager(TieredCacheManager localCacheManager) {
        this.localCacheManager = localCacheManager;
        return this;
    }

    public boolean isUseRemoteCache() {
        return useRemoteCache;
    }

    public CacheConfig setUseRemoteCache(boolean userRemoteCache) {
        this.useRemoteCache = userRemoteCache;
        return this;
    }

    public TieredCacheManager getRemoteCacheManager() {
        return remoteCacheManager;
    }

    public CacheConfig setRemoteCacheManager(TieredCacheManager remoteCacheManager) {
        this.remoteCacheManager = remoteCacheManager;
        return this;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public CacheConfig setSerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }

    public boolean isRefreshable() {
        return refreshable;
    }

    public CacheConfig setRefreshable(boolean refreshable) {
        this.refreshable = refreshable;
        return this;
    }

    public Duration getInterval() {
        return interval;
    }

    public Duration getWarnDelayTime() {
        return warnDelayTime;
    }

    public CacheConfig setWarnDelayTime(Duration warnDelayTime) {
        this.warnDelayTime = warnDelayTime;
        return this;
    }

    public CacheConfig setInterval(Duration interval) {
        this.interval = interval;
        return this;
    }

    public Duration getRefreshTimeout() {
        return refreshTimeout;
    }

    public CacheConfig setRefreshTimeout(Duration refreshTimeout) {
        this.refreshTimeout = refreshTimeout;
        return this;
    }

    public boolean isCacheNullValues() {
        return cacheNullValues;
    }

    public CacheConfig setCacheNullValues(boolean cacheNullValues) {
        this.cacheNullValues = cacheNullValues;
        return this;
    }

    public Duration getLocalExpireTime() {
        return localExpireTime;
    }

    public CacheConfig setLocalExpireTime(Duration localExpireTime) {
        this.localExpireTime = localExpireTime;
        return this;
    }

    public Duration getRemoteExpireTime() {
        return remoteExpireTime;
    }

}
