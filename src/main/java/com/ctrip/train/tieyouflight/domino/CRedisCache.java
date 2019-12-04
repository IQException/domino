package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.config.RedisConfig;
import com.ctrip.train.tieyouflight.domino.support.*;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import credis.java.client.CacheProvider;
import credis.java.client.RedisPubSub;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
public class CRedisCache extends AbstractTieredCache {

    private final static String LOG_TITLE = "CRedisCache";
    private final static byte[] NULL_VALUE = "$$NULL_VALUE$$".getBytes(Charsets.UTF_8);

    private final String name;
    private final CacheProvider cacheProvider;
    private final RedisConfig cacheConfig;
    private final Serializer.Context serializeContext;

    protected CRedisCache(String name, CacheProvider cacheProvider, RedisPubSub redisPubSub,
                          RedisConfig cacheConfig, Type keyType, Type valueType, Type[] parameterTypes) {

        super(cacheConfig.isCacheNullValues(), cacheConfig.isCacheEmptyValues());

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(cacheProvider, "CacheProvider must not be null!");
        Assert.notNull(cacheConfig, "CacheConfig must not be null!");

        this.name = name;
        this.cacheProvider = cacheProvider;
        this.cacheConfig = cacheConfig;
        this.serializeContext = new Serializer.Context(keyType, valueType, parameterTypes);

        new Thread(() -> {
            //cacheProvider.subscribe内部实现：订阅之后会循环获取消息，发送给redisPubSub。
            while (true) {
                try {
                    cacheProvider.subscribe(redisPubSub, CRedisPubSub.getChannel(getName()));
                } catch (Exception e) {
                    ContextAwareClogger.error(LOG_TITLE, e);
                }
            }
        }).start();

    }

    @Override
    public Object get(Object key) {

        byte[] value = cacheProvider.get(createCacheKey(key).getBytes(Charsets.UTF_8));

        if (value == null) {
            return null;
        }

        return deserializeCacheValue(value);
    }

    private String createCacheKey(Object key) {
        return cacheConfig.getKeyWrapper().wrap(cacheConfig.getSerializer().serializeKey(key));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCached(Object key) {
        return cacheProvider.get(createCacheKey(key).getBytes()) != null;
    }

    @Override
    public void put(Object key, @Nullable Object value) {

        Object cacheValue = toStoreValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            ContextAwareClogger.info(LOG_TITLE, String.format(
                    "Cache '%s' does not allow 'null' values. please configure @Domino to allow 'null' via cacheNullValues.",
                    name), ImmutableMap.of("key", cacheConfig.getSerializer().serializeKey(key)));
            return;
        }

        if (!isAllowEmptyValues() && CacheUtil.isEmpty(value)) {
            ContextAwareClogger.info(LOG_TITLE, String.format(
                    "Cache '%s' does not allow 'empty' values. please configure @Domino to allow 'empty' via cacheEmptyValues.",
                    name), ImmutableMap.of("key", cacheConfig.getSerializer().serializeKey(key)));
            return;
        }

        String cacheKey = createCacheKey(key);
        byte[] cacheKeyBytes = cacheKey.getBytes(Charsets.UTF_8);
        if (cacheKeyBytes.length > 1024)
            ContextAwareClogger.error(LOG_TITLE, String.format("key is too long ! credis suggest that key' length should be less than 1024 bytes. current length:%s ", cacheKeyBytes.length),
                    ImmutableMap.of("key", cacheKey));
        cacheProvider.setex(cacheKeyBytes, (int) cacheConfig.getTtl().getSeconds(), serializeCacheValue(cacheValue));
        cacheProvider.publish(CRedisPubSub.getChannel(getName()), cacheKey);
    }

    @Override
    public void evict(Object key) {
        cacheProvider.del(createCacheKey(key).getBytes());
    }

    @Override
    public void clear() {
        //do not clear , use ttl
    }

    protected byte[] serializeCacheValue(Object value) {

        if (isAllowNullValues() && value instanceof NullValue) {
            return NULL_VALUE;
        }
        return cacheConfig.getSerializer().serializeValue(value,serializeContext);
    }

    protected Object deserializeCacheValue(byte[] value) {

        if (isAllowNullValues() && ObjectUtils.nullSafeEquals(value, NULL_VALUE)) {
            return NullValue.INSTANCE;
        }
        return cacheConfig.getSerializer().deserializeValue(value, serializeContext);
    }


}
