package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;
import com.ctrip.train.tieyouflight.domino.support.CacheUtil;
import com.ctrip.train.tieyouflight.domino.support.el.CacheOperationExpressionEvaluator;
import com.ctrip.train.tieyouflight.domino.support.load.LoadingContext;
import com.ctrip.train.tieyouflight.domino.support.load.MergeRequestLoaderCallback;
import com.ctrip.train.tieyouflight.domino.support.load.MergedRequestLoader;
import com.ctrip.train.tieyouflight.domino.support.load.MergedRequestLoaderImpl;
import com.ctrip.train.tieyouflight.domino.support.schedule.RefreshTask;
import com.ctrip.train.tieyouflight.domino.support.schedule.Refresher;
import com.ctrip.train.tieyouflight.domino.support.serialize.KeyParser;
import com.ctrip.train.tieyouflight.domino.support.serialize.SimpleKeyParser;
import com.ctrip.train.tieyouflight.domino.support.stats.DashBoardStatsCounter;
import com.ctrip.train.tieyouflight.domino.support.stats.StatsCounter;
import com.google.common.collect.Lists;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
public class MultiersCache implements ScheduledCache {


    private TieredCache localCache;

    private TieredCache remoteCache;

    private String name;

    private Refresher refresher;

    private CacheConfig cacheConfig;

    private StatsCounter statsCounter;

    private CacheMetadata cacheMetadata;

    private MergedRequestLoader mergedRequestLoader;

    private final AnnotatedElementKey methodCacheKey;

    private final CacheOperationExpressionEvaluator evaluator = new CacheOperationExpressionEvaluator();

    private final KeyParser keyParser = SimpleKeyParser.getInstance();

    public MultiersCache(CacheMetadata cacheMetadata, String name) {
        this.cacheMetadata = cacheMetadata;
        this.cacheConfig = cacheMetadata.getCacheConfig();
        this.name = name;
        if (cacheConfig.isUseLocalCache()) {
            localCache = cacheConfig.getLocalCacheManager().getCache(name, cacheMetadata);
        }
        if (cacheConfig.isUseRemoteCache()) {
            remoteCache = cacheConfig.getRemoteCacheManager().getCache(name, cacheMetadata);
        }
        if (cacheMetadata.getCacheConfig().isRefreshable()) {
            refresher = new Refresher(cacheConfig.getInterval(), cacheConfig.getConcurrency());
        }
        this.statsCounter = new DashBoardStatsCounter(name);
        this.mergedRequestLoader = createMergedLoader();
        this.methodCacheKey = new AnnotatedElementKey(cacheMetadata.getTargetMethod(), cacheMetadata.getBean().getClass());
    }

    protected Object lookup(Object key) {
        //逐层向下查找
        //如果不为null，返回value
        //如果等于null,有两种情况：1. isCacheNullValues 为true，则需要判断这个null是缓存的null（直接返回）还是miss（向下查找）；
        //                      2. isCacheNullValues 为 false,继续向下查找

        if (cacheConfig.isUseLocalCache()) {
            Object value = localCache.get(key);
            if (value == null) {
                if (!cacheConfig.isCacheNullValues()) {
                    this.statsCounter.recordLocalMisses(1);
                } else if (localCache.isCached(key)) {
                    //cached null
                    this.statsCounter.recordLocalHits(1);
                    return value;
                } else {
                    this.statsCounter.recordLocalMisses(1);
                }
            } else {
                this.statsCounter.recordLocalHits(1);
                return value;
            }
        }

        if (cacheConfig.isUseRemoteCache()) {
            Object value = remoteCache.get(key);
            if (value == null) {
                if (!cacheConfig.isCacheNullValues()) {
                    this.statsCounter.recordRemoteMisses(1);
                } else if (remoteCache.isCached(key)) {
                    //cached null
                    if (cacheConfig.isUseLocalCache()) {
                        putLocal(key, value);
                    }
                    this.statsCounter.recordRemoteHits(1);
                    return value;
                } else {
                    this.statsCounter.recordRemoteMisses(1);
                }
            } else {
                if (cacheConfig.isUseLocalCache()) {
                    putLocal(key, value);
                }
                this.statsCounter.recordRemoteHits(1);
                return value;
            }
        }
        return null;
    }

    private void putLocal(Object key, Object value) {

        if (isConditionPassing(key) && !unless(key, value)) {
            localCache.put(key, value);
        } else {
            localCache.evict(key);
        }
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V get(Object key, Callable<V> valueLoader) {

        if (cacheConfig.isRefreshable())
            this.refresher.recordAccess(key);

        V value = null;
        long start = System.currentTimeMillis();
        try {
            if ((value = (V) lookup(key)) == null && cacheConfig.isAutoLoad()) {
                //load
                value = (V) mergedRequestLoader.load(key, new LoadingContext(valueLoader));
                put(key, value);
                this.statsCounter.recordLoadSuccess(Duration.ofMillis(System.currentTimeMillis() - start));
            }
        } catch (Exception e) {
            this.statsCounter.recordLoadException(Duration.ofMillis(System.currentTimeMillis() - start));
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            //schedule
            if (cacheConfig.isRefreshable() && !refresher.contains(key) && isConditionPassing(key)) {
                boolean refresh = false;
                //如果不是autoLoad，先加入刷新队列，刷新之后不满足会踢出队列
                if (!cacheConfig.isAutoLoad()) {
                    refresh = true;
                }
                if (cacheConfig.isAutoLoad() && !unless(key, value)) {
                    refresh = true;
                }
                if (refresh) {
                    RefreshTask task = new RefreshTask<>(this, key, valueLoader,
                            cacheConfig, this.statsCounter, this.refresher);
                    refresher.submit(task);
                    ContextAwareClogger.info("Refresher", String.format("cache : %s , current task number : %s", getName(), refresher.size()));
                }

            }
        }

        return value;
    }

    @Override
    public ValueWrapper get(Object key) {
        throw new UnsupportedOperationException(String.format("cache do not support this operation. " +
                "you may forget to configure @Cacheable(sync = true)! on class: %s, method: %s", cacheMetadata.getBean().getClass(), cacheMetadata.getMethodName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        throw new UnsupportedOperationException(String.format("cache do not support this operation. " +
                "you may forget to configure @Cacheable(sync = true)! on class: %s, method: %s", type, cacheMetadata.getMethodName()));
    }

    @Override
    public void put(Object key, Object value) {
        //先加载到远程，本地缓存在需要时再加载
        //防止破坏本地缓存的缓存策略：本地缓存通常集合比较小，在刷新某个key的时候，本地缓存可能已经不存在这个key了
        if (isConditionPassing(key) && !unless(key, value)) {
            if (cacheConfig.isUseRemoteCache()) {
                remoteCache.put(key, value);
                if (cacheConfig.isUseLocalCache()) {
                    localCache.evict(key);
                }
            } else if (cacheConfig.isUseLocalCache()) {
                localCache.put(key, value);
            }
        }
    }

    @Override
    public boolean refresh(Object key, Object value) {
        //先加载到远程，本地缓存在需要时再加载
        //防止破坏本地缓存的缓存策略：本地缓存通常集合比较小，在刷新某个key的时候，本地缓存可能已经不存在这个key了
        if (isConditionPassing(key) && !unless(key, value)) {
            if (cacheConfig.isUseRemoteCache()) {
                remoteCache.put(key, value);
                if (cacheConfig.isUseLocalCache()) {
                    localCache.evict(key);
                }
            } else if (cacheConfig.isUseLocalCache()) {
                localCache.put(key, value);
            }
            return true;
        } else {
            evict(key);
            return false;
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void evict(Object key) {
        this.statsCounter.recordEviction();
        if (cacheConfig.isUseLocalCache()) {
            localCache.evict(key);
        }
        if (cacheConfig.isUseRemoteCache()) {
            remoteCache.evict(key);
        }
    }

    @Override
    public void clear() {
        if (cacheConfig.isUseLocalCache()) {
            localCache.clear();
        }
        if (cacheConfig.isUseRemoteCache()) {
            remoteCache.clear();
        }
    }

    private <V> MergedRequestLoader<V> createMergedLoader() {
        return new MergedRequestLoaderImpl<V>(
                new MergeRequestLoaderCallback<V>() {

                    @Override
                    public void prepare(Object key, LoadingContext<V> context) {
                    }

                    @Override
                    public void postAfter(Object key, LoadingContext<V> context) {
                    }

                    @Override
                    public V load(Object key, LoadingContext<V> context) throws Exception {
                        return context.getLoader().call();
                    }

                    @Override
                    public V get(Object key, LoadingContext<V> context) {
                        return (V) lookup(key);
                    }

                });
    }

    public void refreshUpdateTime(Object key) {
        refresher.recordUpdate(key);
    }


    private boolean isConditionPassing(Object key) {
        if (StringUtils.hasText(this.cacheConfig.getCond())) {
            EvaluationContext evaluationContext = createEvaluationContext(key, CacheOperationExpressionEvaluator.NO_RESULT);
            return evaluator.condition(this.cacheConfig.getCond(),
                    this.methodCacheKey, evaluationContext);
        }
        return true;
    }

    private boolean unless(Object key, Object result) {
        if (StringUtils.hasText(this.cacheConfig.getExcept())) {
            EvaluationContext evaluationContext = createEvaluationContext(key, result);
            return evaluator.unless(this.cacheConfig.getExcept(), this.methodCacheKey, evaluationContext);
        }
        return false;
    }


    private EvaluationContext createEvaluationContext(Object key, Object result) {
        return evaluator.createEvaluationContext(Lists.newArrayList(this), this.cacheMetadata.getTargetMethod(),
                this.keyParser.parse(this.cacheMetadata.getTargetMethod(), key),
                CacheUtil.getProxyTarget(this.cacheMetadata.getBean()), this.cacheMetadata.getTargetClass(), result, cacheMetadata.getBeanFactory());
    }
}
