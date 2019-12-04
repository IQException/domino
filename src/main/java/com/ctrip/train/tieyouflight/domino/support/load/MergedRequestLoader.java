package com.ctrip.train.tieyouflight.domino.support.load;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public interface MergedRequestLoader<V> {
    /**
     * 装载键为key的value
     *
     * @param key
     * @param context
     * @return
     */
    V load(Object key, LoadingContext<V> context);

    /**
     * 是否正在执行
     *
     * @param key
     * @return
     */
    boolean isRunning(Object key);

}
