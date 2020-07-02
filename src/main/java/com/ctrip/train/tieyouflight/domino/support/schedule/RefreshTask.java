package com.ctrip.train.tieyouflight.domino.support.schedule;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.common.threadmanaged.ThreadLocalVariableManager;
import com.ctrip.train.tieyouflight.domino.ScheduledCache;
import com.ctrip.train.tieyouflight.domino.config.CacheConfig;
import com.ctrip.train.tieyouflight.domino.support.serialize.JsonSerializer;
import com.ctrip.train.tieyouflight.domino.support.stats.StatsCounter;
import com.google.common.collect.Maps;

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

    private ScheduledCache cache;

    private Object key;

    private Instant lastAccess;

    private Instant lastUpdate;

    private Callable<V> callable;

    private int retries;

    private Duration refreshTimeout;

    private ScheduledFuture scheduledFuture;

    private Duration warnDelayTime;

    private StatsCounter statsCounter;

    private Refresher refresher;

    public RefreshTask(ScheduledCache cache, Object key, Callable<V> callable, CacheConfig cacheConfig, StatsCounter statsCounter, Refresher refresher) {
        this(cache, key, null, null, callable, cacheConfig, statsCounter, refresher);
    }


    public RefreshTask(ScheduledCache cache, Object key, Instant lastAccess, Instant lastUpdate, Callable<V> callable, CacheConfig cacheConfig, StatsCounter statsCounter, Refresher refresher) {
        this.cache = cache;
        this.key = key;
        this.lastAccess = lastAccess == null ? Instant.now() : lastAccess;
        this.lastUpdate = lastUpdate == null ? Instant.now() : lastUpdate;
        this.callable = callable;
        this.retries = cacheConfig.getRetries();
        this.refreshTimeout = cacheConfig.getRefreshTimeout();
        this.warnDelayTime = cacheConfig.getWarnDelayTime();
        this.statsCounter = statsCounter;
        this.refresher = refresher;
        logTags.put("cache", cache.getName());
        logTags.put("key", JsonSerializer.getInstance().serializeKey(key));
    }

    @Override
    public void run() {

        String taskId = UUID.randomUUID().toString();
        ThreadLocalVariableManager.addLogTag("taskId", taskId);
        try {
            //如果已被更新过（其他实例更新同步刷新了）
            if (lastUpdate.plusSeconds(refresher.getInterval().getSeconds())
                    .compareTo(Instant.now()) > 0) {
                return;
            }
            //长时间无访问，则删除刷新任务
            if (lastAccess.plusSeconds(refreshTimeout.getSeconds())
                    .compareTo(Instant.now()) < 0) {
                refresher.remove(this);
                return;
            }

            if (scheduledFuture != null && scheduledFuture.getDelay(TimeUnit.SECONDS) + warnDelayTime.getSeconds() <= 0) {
                ContextAwareClogger.error(LOG_TITLE, String.format("delayed to load cache. delay: %s s.  possible reasons: " +
                                "1. method execute too slow ; 2. @Domino(concurrency) is too small ; 3. @Domino(localSize) is too large. ",
                        scheduledFuture.getDelay(TimeUnit.SECONDS)), logTags);
            }
            long start = System.currentTimeMillis();
            RetryTemplate.RetryResult<V> retryResult = RetryTemplate.tryTodo(callable, retries);
            if (!retryResult.isSuccess()) {
                this.statsCounter.recordLoadException(Duration.ofMillis(System.currentTimeMillis() - start));
                ContextAwareClogger.warn(LOG_TITLE, String.format("failed to load cache after retrying %s times," +
                        "please check warn message in the same thread or by the tag: taskId=%s", retries, taskId), logTags);
            } else {
                this.statsCounter.recordLoadSuccess(Duration.ofMillis(System.currentTimeMillis() - start));
                ContextAwareClogger.info(LOG_TITLE, "load cache success .", logTags);
                boolean refreshResult = cache.refresh(key, retryResult.getResult());
                if (!refreshResult) {
                    //刷新失败，删除任务
                    refresher.remove(this);
                }
                this.lastUpdate = Instant.now();
            }
        } catch (Throwable t) {
            ContextAwareClogger.error(LOG_TITLE, t, logTags);
        } finally {
            ThreadLocalVariableManager.removeLogTag("taskId");
        }
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public ScheduledFuture getScheduledFuture() {
        return scheduledFuture;
    }

    public RefreshTask<V> setScheduledFuture(ScheduledFuture scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
        return this;
    }

    public ScheduledCache getCache() {
        return cache;
    }

    public RefreshTask<V> setCache(ScheduledCache cache) {
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

    public RefreshTask<V> setLastAccess(Instant lastAccess) {
        this.lastAccess = lastAccess;
        return this;
    }

}
