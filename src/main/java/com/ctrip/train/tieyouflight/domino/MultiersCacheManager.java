package com.ctrip.train.tieyouflight.domino;

import com.ctrip.framework.foundation.Foundation;
import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;
import com.ctrip.train.tieyouflight.domino.config.Domino;
import com.ctrip.train.tieyouflight.domino.config.MultiersCacheProperties;
import com.ctrip.train.tieyouflight.domino.support.CRedisPubSub;
import com.ctrip.train.tieyouflight.domino.support.serialize.JsonSerializer;
import com.ctrip.train.tieyouflight.domino.support.serialize.KeyWrapper;
import com.ctrip.train.tieyouflight.domino.support.serialize.Serializer;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import qunar.tc.qconfig.client.MapConfig;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
public class MultiersCacheManager extends AbstractCacheManager implements ApplicationContextAware, ApplicationListener {

    private final String KEY_JOINER = "::";

    private BeanDefinitionRegistry registry;

    private ApplicationContext applicationContext;

    private TieredCacheManager localCacheManager;

    private TieredCacheManager remoteCacheManager;

    private CacheManager springCacheManager;

    private MultiersCacheProperties multiersCacheProperties;

    private final Set<String> loadOnStartUpCaches = Sets.newHashSet();

    private final ConcurrentMap<String, CacheMetadata> cacheMetadataMap = Maps.newConcurrentMap();

    private final ConcurrentMap<String, Cache> cacheMap = Maps.newConcurrentMap();

    public MultiersCacheManager(MultiersCacheProperties multiersCacheProperties,
                                TieredCacheManager localCacheManager,
                                TieredCacheManager remoteCacheManager,
                                CacheManager springCacheManager) {

        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = remoteCacheManager;
        this.springCacheManager = springCacheManager;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<Cache> caches = Lists.newArrayListWithCapacity(loadOnStartUpCaches.size());
        for (String cacheName : loadOnStartUpCaches) {
            caches.add(getCache(cacheName));
        }
        return caches;
    }

    public Cache getCache(String name) {
        CacheMetadata cacheMetadata = cacheMetadataMap.get(name);
        if (cacheMetadata == null) {
            return springCacheManager.getCache(name);
        }
        return cacheMap.computeIfAbsent(name, key -> new MultiersCache(cacheMetadata, key));
    }

    public Collection<String> getCacheNames() {
        return Collections.emptyList();
    }


    /**
     * evict t1 cache keyStr
     *
     * @param cacheName
     * @param keyStr
     */
    public void evictLocalCache(String cacheName, String keyStr) {
        CacheMetadata metadata = cacheMetadataMap.get(cacheName);
        if (metadata.getCacheConfig().isUseLocalCache()) {
            TieredCache localCache = localCacheManager.getCache(cacheName, metadata);
            Serializer.Context context = new Serializer.Context(metadata.getKeyType(), metadata.getValueType(), metadata.getMethodParamTypes());
            Object key = metadata.getCacheConfig().getSerializer().deserializeKey(metadata.getCacheConfig().getKeyWrapper().unwrap(keyStr), context);
            localCache.evict(key);
        }
    }


    public void refreshUpdateTime(String cacheName, String keyStr) {
        CacheMetadata metadata = cacheMetadataMap.get(cacheName);
        if (metadata.getCacheConfig().isRefreshable()) {
            MultiersCache cache = (MultiersCache) getCache(cacheName);
            Serializer.Context context = new Serializer.Context(metadata.getKeyType(), metadata.getValueType(), metadata.getMethodParamTypes());
            Object key = metadata.getCacheConfig().getSerializer().deserializeKey(metadata.getCacheConfig().getKeyWrapper().unwrap(keyStr), context);
            cache.refreshUpdateTime(key);
        }

    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.registry = (BeanDefinitionRegistry) applicationContext;
        String[] beanNames = registry.getBeanDefinitionNames();
        for (String beanName : beanNames) {

            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (className == null) continue;

            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                ContextAwareClogger.warn("domino", e);
            }
            while (clazz != null) {
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {

                    Domino domino = AnnotationUtils.findAnnotation(method, Domino.class);
                    if (domino == null) continue;
                    //不支持集合类型参数
                    validateParamTypes(clazz, method);
                    String errorSuffix = String.format(" On class : %s, method : %s ", clazz, method);

                    String cacheName = domino.name();
                    if (cacheMetadataMap.containsKey(cacheName))
                        throw new RuntimeException("do not support sharing cache among several methods,'cause different method has different args,need different serializer");

                    CacheConfig cacheConfig = buildCacheConfig(cacheName, domino, errorSuffix);
                    validateCacheConfig(cacheConfig);

                    //FIXME 丑陋实现
                    if (cacheConfig.isUseRemoteCache() && cacheConfig.getRemoteCacheManager() instanceof CRedisCacheManager) {
                        CRedisCacheManager cRedisCacheManager = (CRedisCacheManager) cacheConfig.getRemoteCacheManager();
                        cRedisCacheManager.setRedisPubSub(new CRedisPubSub(this));
                    }

                    CacheMetadata cacheMetadata = new CacheMetadata();
                    cacheMetadata.setBeanFactory(applicationContext);
                    cacheMetadata.setCacheConfig(cacheConfig);
                    cacheMetadata.setTargetClass(clazz);
                    cacheMetadata.setTargetMethod(method);
                    cacheMetadata.setBean(applicationContext.getBean(beanName));

                    if (Optional.class == method.getReturnType()) {
                        ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
                        cacheMetadata.setValueType(parameterizedType.getActualTypeArguments()[0]);
                    } else {
                        cacheMetadata.setValueType(wrapPrimitiveType(method.getGenericReturnType()));
                    }
                    //见 SimpleKeyGenerator
                    if (method.getParameterTypes().length == 1) {
                        cacheMetadata.setKeyType(wrapPrimitiveType(method.getGenericParameterTypes()[0]));
                    } else {
                        cacheMetadata.setKeyType(SimpleKey.class);
                    }
                    cacheMetadata.setMethodParamTypes(wrapPrimitiveType(method.getGenericParameterTypes()));
                    cacheMetadataMap.put(cacheName, cacheMetadata);

                    if (domino.loadOnStartUp()) {
                        Type[] paramType = method.getGenericParameterTypes();
                        if (paramType != null && paramType.length > 0) {
                            throw new RuntimeException(String.format("only support loadOnStartUp on methods that have no param signature. %s", errorSuffix));
                        }
                        loadOnStartUpCaches.add(cacheName);
                    }

                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    private Type wrapPrimitiveType(Type type) {
        if (type instanceof Class) {
            Class _type = (Class) type;
            if (_type.isPrimitive())
                return ClassUtils.primitiveToWrapper(_type);
        }
        return type;
    }

    private Type[] wrapPrimitiveType(Type[] types) {
        if (ArrayUtils.isEmpty(types)) return types;
        Type[] _types = new Type[types.length];
        int i = 0;
        for (Type type : types) {
            _types[i++] = wrapPrimitiveType(type);
        }
        return _types;
    }

    private CacheConfig buildCacheConfig(String cacheName, Domino domino, String errorSuffix) {
        Map<String, String> dominoQc = MapConfig.get("domino.t").asMap();
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setAutoLoad(domino.autoLoad())
                .setCacheNullValues(domino.cacheNullValues())
                .setCacheEmptyValues(domino.cacheEmptyValues())
                .setRefreshable(domino.refreshable())
                .setKeyWrapper(buildKeyWrapper(cacheName, domino.keyPrefix()))
                .setUseLocalCache(domino.useLocalCache())
                .setUseRemoteCache(domino.useRemoteCache())
                .setExcept(domino.except())
                .setCond(domino.cond());

        if (domino.refreshable()) {
            if (domino.retries() == Domino.UNSET_INT) {
                Integer retries = MapUtils.getInteger(dominoQc, cacheName + "/retries");
                if (retries == null) {
                    cacheConfig.setRetries(multiersCacheProperties.getRetries());
                } else {
                    cacheConfig.setRetries(retries);
                }
            } else {
                cacheConfig.setRetries(domino.retries());
            }
            if (domino.concurrency() == Domino.UNSET_INT) {
                Integer concurrency = MapUtils.getInteger(dominoQc, cacheName + "/concurrency");
                if (concurrency == null) {
                    cacheConfig.setConcurrency(multiersCacheProperties.getConcurrency());
                } else {
                    cacheConfig.setConcurrency(concurrency);
                }
            } else {
                cacheConfig.setConcurrency(domino.concurrency());
            }
            if (domino.interval() == Domino.UNSET_LONG) {
                Long interval = MapUtils.getLong(dominoQc, cacheName + "/interval");
                if (interval == null) {
                    cacheConfig.setInterval(Duration.ofSeconds(multiersCacheProperties.getInterval()));
                } else {
                    cacheConfig.setInterval(Duration.ofSeconds(interval));
                }
            } else {
                cacheConfig.setInterval(Duration.ofSeconds(domino.interval()));
            }
            if (domino.warnDelayTime() == Domino.UNSET_LONG) {
                Long warnDelayTime = MapUtils.getLong(dominoQc, cacheName + "/warnDelayTime");
                if (warnDelayTime == null) {
                    cacheConfig.setWarnDelayTime(cacheConfig.getInterval());
                } else {
                    cacheConfig.setWarnDelayTime(Duration.ofSeconds(warnDelayTime));
                }
            } else {
                cacheConfig.setWarnDelayTime(Duration.ofSeconds(domino.warnDelayTime()));
            }

            if (domino.refreshTimeout() == Domino.UNSET_LONG) {
                Long refreshTimeout = MapUtils.getLong(dominoQc, cacheName + "/refreshTimeout");
                if (refreshTimeout == null) {
                    cacheConfig.setRefreshTimeout(cacheConfig.getInterval().multipliedBy(2));
                } else {
                    cacheConfig.setRefreshTimeout(Duration.ofSeconds(Math.min(refreshTimeout, 3600 * 24 * 180)));
                }
            } else {
                cacheConfig.setRefreshTimeout(Duration.ofSeconds(Math.min(domino.refreshTimeout(), 3600 * 24 * 180)));
            }
        }

        if (domino.useLocalCache()) {
            if (domino.localSize() == Domino.UNSET_LONG) {
                Long localSize = MapUtils.getLong(dominoQc, cacheName + "/localSize");
                if (localSize == null) {
                    cacheConfig.setLocalSize(multiersCacheProperties.getLocalSize());
                } else {
                    cacheConfig.setLocalSize(localSize);
                }
            } else {
                cacheConfig.setLocalSize(domino.localSize());
            }

            if (domino.localExpireTime() == Domino.UNSET_LONG) {
                Long localExpireTime = MapUtils.getLong(dominoQc, cacheName + "/localExpireTime");
                if (localExpireTime == null) {
                    cacheConfig.setLocalExpireTime(Duration.ofSeconds(multiersCacheProperties.getLocalExpireTime()));
                } else {
                    cacheConfig.setLocalExpireTime(Duration.ofSeconds(localExpireTime));
                }
            } else {
                cacheConfig.setLocalExpireTime(Duration.ofSeconds(domino.localExpireTime()));
            }
            String lcmName = domino.localCacheManager();
            if (StringUtils.isNotBlank(lcmName)) {
                TieredCacheManager customLcm = applicationContext.getBean(lcmName, TieredCacheManager.class);
                if (customLcm == null) {
                    throw new RuntimeException(String.format("cannot find localCacheManager : %s. %s", lcmName, errorSuffix));
                }
                cacheConfig.setLocalCacheManager(customLcm);
            } else {
                if (this.localCacheManager == null) {
                    throw new RuntimeException(String.format("cannot find localCacheManager. %s", errorSuffix));
                }
                cacheConfig.setLocalCacheManager(this.localCacheManager);
            }
        }
        if (domino.useRemoteCache()) {
            if (domino.remoteExpireTime() == Domino.UNSET_LONG) {
                Long remoteExpireTime = MapUtils.getLong(dominoQc, cacheName + "/remoteExpireTime");
                if (remoteExpireTime == null) {
                    cacheConfig.setRemoteExpireTime(Duration.ofSeconds(multiersCacheProperties.getRemoteExpireTime()));
                } else {
                    cacheConfig.setRemoteExpireTime(Duration.ofSeconds(remoteExpireTime));
                }
            } else {
                cacheConfig.setRemoteExpireTime(Duration.ofSeconds(domino.remoteExpireTime()));
            }
            String rcmName = domino.remoteCacheManager();
            if (StringUtils.isNotBlank(rcmName)) {
                TieredCacheManager customRcm = applicationContext.getBean(rcmName, TieredCacheManager.class);
                if (customRcm == null) {
                    throw new RuntimeException(String.format("cannot find remoteCacheManager : %s. %s", rcmName, errorSuffix));
                }
                cacheConfig.setRemoteCacheManager(customRcm);
            } else {
                if (this.remoteCacheManager == null) {
                    throw new RuntimeException(String.format("cannot find remoteCacheManager. %s", errorSuffix));
                }
                cacheConfig.setRemoteCacheManager(this.remoteCacheManager);
            }
        }
        if (StringUtils.isNotBlank(domino.serializer())) {
            Serializer serializer = applicationContext.getBean(domino.serializer(), Serializer.class);
            if (serializer == null) {
                throw new RuntimeException(String.format("cannot find serializer : %s. %s", domino.serializer(), errorSuffix));
            }
            cacheConfig.setSerializer(serializer);
        } else {
            cacheConfig.setSerializer(new JsonSerializer());
        }

        return cacheConfig;
    }

    private KeyWrapper buildKeyWrapper(String cacheName, String keyPrefix) {
        return new KeyWrapper() {
            @Override
            public String wrap(String originKey) {
                return Joiner.on(KEY_JOINER).join(Foundation.app().getAppId(), cacheName, keyPrefix, originKey);
            }

            @Override
            public String unwrap(String wrappedKey) {
                int keyLength = Joiner.on(KEY_JOINER).join(Foundation.app().getAppId(), cacheName, keyPrefix, "").length();
                return wrappedKey.substring(keyLength);
            }
        };
    }


    private void validateCacheConfig(CacheConfig cacheConfig) {
        if (cacheConfig.isUseLocalCache()) {
            Preconditions.checkArgument(cacheConfig.getLocalExpireTime().getSeconds() >= 0, "localExpireTime cannot be negative !");
            Preconditions.checkArgument(cacheConfig.getLocalSize() >= 0, "localSize cannot be negative !");
        }
        if (cacheConfig.isUseRemoteCache()) {
            Preconditions.checkArgument(cacheConfig.getRemoteExpireTime().getSeconds() > 0, "remoteExpireTime must be larger than 0!");
        }

        if (cacheConfig.isRefreshable()) {
            Preconditions.checkArgument(cacheConfig.getInterval().getSeconds() > 0, "interval must be larger than 0 !");
            Preconditions.checkArgument(cacheConfig.getRetries() >= 0, "retries cannot be negative!");
            Preconditions.checkArgument(cacheConfig.getRefreshTimeout().getSeconds() > 0, "refreshTimeout must be larger than 0 !");
            Preconditions.checkArgument(cacheConfig.getConcurrency() > 0, "concurrency must be larger than 0 !");
            Preconditions.checkArgument(cacheConfig.getWarnDelayTime().getSeconds() > 0, "warnDelayTime must be larger than 0 !");
        }
    }

    private void validateParamTypes(Class<?> clazz, Method method) {

        for (Class<?> paramType : method.getParameterTypes()) {
            if (Collection.class.isAssignableFrom(paramType) || Map.class.isAssignableFrom(paramType) || clazz.isArray()) {
                throw new IllegalArgumentException(
                        String.format("domino do not support Collection or Map or Array paramType and param's field type (recursively)," +
                                "because the key will be over large and result in cache miss ! in class : %s, method : %s ", clazz, method));
            }
            if (clazz != paramType) {
                validateCacheKeyType(paramType);
            }
        }

    }

    private void validateCacheKeyType(Class<?> clazz) {

        if (clazz == Object.class || clazz == String.class || ClassUtils.isPrimitiveOrWrapper(clazz))
            return;

        ReflectionUtils.doWithFields(clazz, field -> {
            if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()) || clazz.isArray()) {
                throw new IllegalArgumentException(
                        String.format("domino do not support Collection or Map or Array paramType and param's field type (recursively)," +
                                "because the key will be over large and result in cache miss ! in class : %s, field : %s ", clazz, field));
            }
            if (field.getType() != clazz) {
                validateCacheKeyType(field.getType());
            }
        });

    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            for (String cacheName : loadOnStartUpCaches) {
                CacheMetadata cacheMetadata = cacheMetadataMap.get(cacheName);
                try {
                    Method method = cacheMetadata.getBean().getClass().getMethod(cacheMetadata.getMethodName());
                    method.invoke(cacheMetadata.getBean());
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
    }
}
