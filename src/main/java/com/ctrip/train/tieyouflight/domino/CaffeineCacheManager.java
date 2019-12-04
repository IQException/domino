package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;
import com.ctrip.train.tieyouflight.domino.config.CaffeineConfig;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheManager implements TieredCacheManager {

    private final ConcurrentMap<String, TieredCache> cacheMap = Maps.newConcurrentMap();

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }

    @Override
    public TieredCache getCache(String name, CacheMetadata metadata) {

        return cacheMap.computeIfAbsent(name, key -> {
            CaffeineConfig caffeineConfig = new CaffeineConfig();
            CacheConfig cacheConfig = metadata.getCacheConfig();
            caffeineConfig.setCacheNullValues(cacheConfig.isCacheNullValues())
                    .setCacheEmptyValues(cacheConfig.isCacheEmptyValues())
                    .setMaxSize(cacheConfig.getLocalSize())
                    .setSerializer(cacheConfig.getSerializer())
                    .setTtl(cacheConfig.getLocalExpireTime());
            return createCaffeineCache(name, caffeineConfig);
        });
    }

    protected TieredCache createCaffeineCache(String name, CaffeineConfig caffeineConfig) {
        return new CaffeineCache(name,caffeineConfig);
    }


}