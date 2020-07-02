package com.ctrip.train.tieyouflight.domino.support.load;

import java.util.concurrent.Callable;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class LoadingContext<V> {

    private Callable<V> loader;

    public Callable<V> getLoader() {
        return loader;
    }

    public LoadingContext(Callable<V> loader) {
        this.loader = loader;
    }
}
