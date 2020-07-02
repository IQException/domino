package com.ctrip.train.tieyouflight.domino.support.load;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public interface MergedKey<T> {

    int hash(LoadingContext context);

    boolean equalsKey(T other, LoadingContext context);

    T getKey();
}
