package com.ctrip.train.tieyouflight.domino.support.load;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public interface MergeRequestLoaderCallback<V> {

    void prepare(Object key, LoadingContext<V> context);

    void postAfter(Object key, LoadingContext<V> context);

    V load(Object key, LoadingContext<V> context) throws Exception;

    V get(Object key, LoadingContext<V> context);
}
