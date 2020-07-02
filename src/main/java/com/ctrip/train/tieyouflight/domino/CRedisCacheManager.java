/*
 * Copyright 2011-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;
import com.ctrip.train.tieyouflight.domino.config.RedisConfig;
import com.google.common.collect.Maps;
import credis.java.client.CacheProvider;
import credis.java.client.RedisPubSub;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
public class CRedisCacheManager implements TieredCacheManager {

    private CacheProvider cacheProvider;

    private RedisPubSub redisPubSub;

    private final ConcurrentMap<String, TieredCache> cacheMap = Maps.newConcurrentMap();

    public CRedisCacheManager(CacheProvider cacheProvider, RedisPubSub redisPubSub) {

        Assert.notNull(cacheProvider, "CacheProvider must not be null!");

        this.cacheProvider = cacheProvider;
        this.redisPubSub = redisPubSub;
    }

    public CRedisCacheManager(CacheProvider cacheProvider) {

        Assert.notNull(cacheProvider, "CacheProvider must not be null!");

        this.cacheProvider = cacheProvider;
    }

    public void setRedisPubSub(RedisPubSub redisPubSub) {
        this.redisPubSub = redisPubSub;
    }

    protected CRedisCache createRedisCache(String name, RedisConfig cacheConfig, Type keyType, Type valueType, Type[] parameterTypes) {
        return new CRedisCache(name, cacheProvider, redisPubSub, cacheConfig, keyType, valueType, parameterTypes);
    }

    @Override
    public TieredCache getCache(String name, CacheMetadata cacheMetadata) {
        return cacheMap.computeIfAbsent(name, key -> {
            RedisConfig redisCacheConfig = new RedisConfig();
            CacheConfig cacheConfig = cacheMetadata.getCacheConfig();
            redisCacheConfig.setCacheNullValues(cacheConfig.isCacheNullValues())
                    .setCacheEmptyValues(cacheConfig.isCacheEmptyValues())
                    .setKeyWrapper(cacheConfig.getKeyWrapper())
                    .setSerializer(cacheConfig.getSerializer())
                    .setTtl(cacheConfig.getRemoteExpireTime())
                    .setPubsub(cacheConfig.isUseLocalCache());
            return createRedisCache(name, redisCacheConfig, cacheMetadata.getKeyType(), cacheMetadata.getValueType(), cacheMetadata.getMethodParamTypes());
        });
    }


    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }
}
