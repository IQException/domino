package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.config.CaffeineConfig;
import com.ctrip.train.tieyouflight.domino.support.AbstractTieredCache;
import com.ctrip.train.tieyouflight.domino.support.CacheUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.TimeUnit;

/**
 * @author wang.wei
 * @since 2019/5/15
 */
public class CaffeineCache extends AbstractTieredCache {
    private final static String LOG_TITLE = "CaffeineCache";
    private final String name;
    private final CaffeineConfig caffeineConfig;

    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

    public CaffeineCache(String name, CaffeineConfig caffeineConfig) {
        super(caffeineConfig.isCacheNullValues(), caffeineConfig.isCacheEmptyValues());
        this.caffeineConfig = caffeineConfig;
        this.name = name;
        this.cache = Caffeine.newBuilder().expireAfterWrite(caffeineConfig.getTtl().getSeconds(), TimeUnit.SECONDS)
                .maximumSize(caffeineConfig.getMaxSize())
                .build();
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public boolean isCached(Object key) {
        return get(key) != null;
    }

    @Override
    public Object get(Object key) {
        if (this.cache instanceof LoadingCache) {
            return ((LoadingCache<Object, Object>) this.cache).get(key);
        }
        return this.cache.getIfPresent(key);
    }

    @Override
    public void put(Object key, Object value) {

        Object cacheValue = toStoreValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            ContextAwareClogger.info(LOG_TITLE, String.format(
                    "Cache '%s' does not allow 'null' values. please configure @Domino to allow 'null' via cacheNullValues.",
                    name), ImmutableMap.of("key", caffeineConfig.getSerializer().serializeKey(key)));
            return;
        }

        if (!isAllowEmptyValues() && CacheUtil.isEmpty(value)) {
            ContextAwareClogger.info(LOG_TITLE, String.format(
                    "Cache '%s' does not allow 'empty' values. please configure @Domino to allow 'empty' via cacheEmptyValues.",
                    name), ImmutableMap.of("key", caffeineConfig.getSerializer().serializeKey(key)));
            return;
        }
        this.cache.put(key, cacheValue);
    }


    @Override
    public void evict(Object key) {
        this.cache.invalidate(key);
    }

    @Override
    public void clear() {
        this.cache.invalidateAll();
    }

}
