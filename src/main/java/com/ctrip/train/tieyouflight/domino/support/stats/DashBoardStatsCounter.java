package com.ctrip.train.tieyouflight.domino.support.stats;

import com.ctrip.framework.clogging.agent.metrics.aggregator.MetricsAggregator;
import com.ctrip.framework.clogging.agent.metrics.aggregator.MetricsAggregatorFactory;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

/**
 * @author wang.wei
 * @since 2019/6/30
 */
public class DashBoardStatsCounter implements StatsCounter {

    private MetricsAggregator cacheMetrics;

    private String cacheName;

    public DashBoardStatsCounter(String cacheName) {
        this.cacheName = cacheName;
        this.cacheMetrics = MetricsAggregatorFactory.createAggregator("domino", "cache","dime").enableMax().enableMin();

    }

    private void recordHits(int count) {
        this.cacheMetrics.add(count, this.cacheName,"hit");
    }

    @Override
    public void recordLocalHits(int count) {
        this.cacheMetrics.add(count, this.cacheName,"hit.local");
        recordHits(count);
    }

    @Override
    public void recordRemoteHits(int count) {
        this.cacheMetrics.add(count, this.cacheName,"hit.remote");
        recordHits(count);
    }

    private void recordMisses(int count) {
        this.cacheMetrics.add(count, this.cacheName,"miss");
    }

    @Override
    public void recordLocalMisses(int count) {
        this.cacheMetrics.add(count, this.cacheName,"miss.local");
    }

    @Override
    public void recordRemoteMisses(int count) {
        this.cacheMetrics.add(count, this.cacheName,"miss.remote");
        recordMisses(count);
    }

    @Override
    public void recordLoadSuccess(Duration loadTime) {
        this.cacheMetrics.add(loadTime.toMillis(), this.cacheName,"load.time");
        this.cacheMetrics.add(1,this.cacheName,"load");
    }

    @Override
    public void recordLoadException(Duration loadTime) {
        this.cacheMetrics.add(loadTime.toMillis(), this.cacheName,"error.time");
        this.cacheMetrics.add(1,this.cacheName,"error");
    }

    @Override
    public void recordEviction() {
        this.cacheMetrics.add(1, this.cacheName,"evict");
    }

}
