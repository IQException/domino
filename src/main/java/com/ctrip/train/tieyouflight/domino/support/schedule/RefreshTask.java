package com.ctrip.train.tieyouflight.domino.support.schedule;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.common.threadmanaged.ThreadLocalVariableManager;
import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.support.CacheUtil;
import com.ctrip.train.tieyouflight.domino.support.JsonSerializer;
import com.ctrip.train.tieyouflight.domino.support.stats.StatsCounter;
import com.google.common.collect.Maps;
import org.springframework.cache.Cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author wang.wei
 * @since 2019/6/27
 */
public class RefreshTask<V> implements Runnable {

    private static final String LOG_TITLE = "RefreshTask";

    private final Map<String, String> logTags = Maps.newHashMap();

    private Cache cache;

    private Object key;

    private Instant lastAccess;

    private Callable<V> callable;

    private int retries;

    private boolean cacheNullValues;

    private boolean cacheEmptyValues;

    private Duration refreshTimeout;

    private ScheduledFuture scheduledFuture;

    private Duration warnDelayTime;

    private StatsCounter statsCounter;

    private Refresher refresher;

    public RefreshTask(Cache cache, Object key, Instant lastAccess, Callable<V> callable, CacheConfig cacheConfig, StatsCounter statsCounter, Refresher refresher) {
        this.cache = cache;
        this.key = key;
        this.lastAccess = lastAccess == null ? Instant.now() : lastAccess;
        this.callable = callable;
        this.retries = cacheConfig.getRetries();
        this.refreshTimeout = cacheConfig.getRefreshTimeout();
        this.warnDelayTime = cacheConfig.getWarnDelayTime();
        this.cacheNullValues = cacheConfig.isCacheNullValues();
        this.cacheEmptyValues = cacheConfig.isCacheEmptyValues();
        this.statsCounter = statsCounter;
        this.refresher = refresher;
        logTags.put("cache", cache.getName());
        logTags.put("key", JsonSerializer.getInstance().serializeKey(key));
    }

    @Override
    public void run() {

        String taskId = UUID.randomUUID().toString();
        ThreadLocalVariableManager.addLogTag("taskId",taskId);
        try {
            if (lastAccess.plusSeconds(refreshTimeout.getSeconds())
                    .compareTo(Instant.now()) < 0)
                return;
            if (scheduledFuture != null && scheduledFuture.getDelay(TimeUnit.SECONDS) + warnDelayTime.getSeconds() <= 0) {
                ContextAwareClogger.error(LOG_TITLE, String.format("delayed to load cache. delay: %s s.  possible reasons: " +
                                "1. method execute too slow ; 2. @Domino(concurrency) is too small ; 3. @Domino(localSize) is too large. ",
                        scheduledFuture.getDelay(TimeUnit.SECONDS)), logTags);
                refresher.remove(this);
            }
            long start = System.currentTimeMillis();
            RetryTemplate.RetryResult<V> retryResult = RetryTemplate.tryTodo(callable
                    , ret -> (!this.cacheNullValues && ret == null) || (!this.cacheEmptyValues && CacheUtil.isEmpty(ret))
                    , retries);
            if (!retryResult.isSuccess()) {
                this.statsCounter.recordLoadException(Duration.ofMillis(System.currentTimeMillis() - start));
                ContextAwareClogger.warn(LOG_TITLE, String.format("failed to load cache after retrying %s times," +
                        "please check warn message in the same thread or by the tag: taskId=%s", retries,taskId), logTags);
            } else {
                this.statsCounter.recordLoadSuccess(Duration.ofMillis(System.currentTimeMillis() - start));
                ContextAwareClogger.info(LOG_TITLE, "load cache success .", logTags);
                cache.put(key, retryResult.getResult());
            }
        } catch (Throwable t) {
            ContextAwareClogger.error(LOG_TITLE, t, logTags);
        }finally {
            ThreadLocalVariableManager.removeLogTag("taskId");
        }
    }

    public ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }

    public RefreshTask<V> setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
        return this;
    }

    public Cache getCache() {
        return cache;
    }

    public RefreshTask<V> setCache(Cache cache) {
        this.cache = cache;
        return this;
    }

    public Object getKey() {
        return key;
    }

    public RefreshTask<V> setKey(Object key) {
        this.key = key;
        return this;
    }

    public Instant getLastAccess() {
        return lastAccess;
    }

    public RefreshTask<V> setLastAccess(Instant lastAccess) {
        this.lastAccess = lastAccess;
        return this;
    }

    public Callable<V> getCallable() {
        return callable;
    }

    public RefreshTask<V> setCallable(Callable<V> callable) {
        this.callable = callable;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public RefreshTask<V> setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public Duration getRefreshTimeout() {
        return refreshTimeout;
    }

    public RefreshTask<V> setRefreshTimeout(Duration refreshTimeout) {
        this.refreshTimeout = refreshTimeout;
        return this;
    }
}
