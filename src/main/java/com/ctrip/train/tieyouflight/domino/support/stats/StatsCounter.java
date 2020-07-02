package com.ctrip.train.tieyouflight.domino.support.stats;


import java.time.Duration;

/**
 * @author wang.wei
 * @since 2019/6/30
 */
public interface StatsCounter {

    void recordLocalHits(int count);

    void recordRemoteHits(int count);

    void recordLocalMisses(int count);

    void recordRemoteMisses(int count);

    void recordLoadSuccess(Duration loadTime);

    void recordLoadException(Duration loadTime);

    void recordEviction();

}
