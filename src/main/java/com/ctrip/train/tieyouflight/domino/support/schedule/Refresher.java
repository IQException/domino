package com.ctrip.train.tieyouflight.domino.support.schedule;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author wang.wei
 * @since 2019/6/27
 */
public class Refresher {
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private Duration interval;
    private Map<Object, RefreshTask> tasks = new ConcurrentHashMap<>(1024);

    public Refresher(Duration interval, int concurrency) {
        this.interval = interval;
        this.scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) Executors
                .newScheduledThreadPool(concurrency, new DefaultThreadFactory("Domino-Refresh-Thread"));
        this.scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduledThreadPoolExecutor.shutdownNow()));
    }

    public boolean contains(Object key) {
        return tasks.containsKey(key);
    }

    public boolean submit(RefreshTask task) {
        Object key = task.getKey();
        RefreshTask existTask = this.tasks.putIfAbsent(key, task);
        // put success
        if (existTask == null) {
            existTask = this.tasks.get(key);
            long lazySec = ThreadLocalRandom.current().nextLong(this.interval.getSeconds());
            ScheduledFuture future = this.scheduledThreadPoolExecutor
                    .scheduleWithFixedDelay(task, lazySec, this.interval.getSeconds(),
                            TimeUnit.SECONDS);
            existTask.setScheduledFuture(future);
            ContextAwareClogger.info(
                    String.format("Schedule task: %s, lazy seconds: %s, interval seconds: %s ",
                            key, lazySec, this.interval));
            return true;
        }
        return false;
    }

    public Duration getInterval() {
        return interval;
    }

    public void remove(RefreshTask task) {
        task = tasks.remove(task.getKey());
        if (task != null) {
            task.getScheduledFuture().cancel(true);
        }
    }

    public void recordAccess(Object key) {
        RefreshTask task = tasks.get(key);
        if (task != null)
            task.setLastAccess(Instant.now());
    }

    public void recordUpdate(Object key) {
        RefreshTask task = tasks.get(key);
        if (task != null)
            task.setLastUpdate(Instant.now());
    }

    public int size() {
        return tasks.size();
    }
}
