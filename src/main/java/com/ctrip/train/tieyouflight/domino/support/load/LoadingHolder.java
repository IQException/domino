package com.ctrip.train.tieyouflight.domino.support.load;

import java.util.concurrent.ExecutionException;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public interface LoadingHolder<K, V> {

    LoadingEntry<K, V> waitForValue() throws ExecutionException;

    void notifyNewValue(LoadingEntry<K, V> entry);

    boolean isLoading();

    void setLoading(boolean loading);

    void recreateFutureTask();

    public class LoadingEntry<K, V> {
        private MergedKey<K> key;
        private V value;
        private Throwable t;

        public LoadingEntry(MergedKey<K> key) {
            this.key = key;
            this.value = null;
        }

        public LoadingEntry(MergedKey<K> key, V value) {
            this.key = key;
            this.value = value;
        }

        public void setValue(V value) {
            this.value = value;
        }

        public MergedKey<K> getKey() {
            return key;
        }

        public K getMergedKeyValue() {
            return key.getKey();
        }

        public V getValue() {
            return value;
        }

        public Throwable getT() {
            return t;
        }

        public void setT(Throwable t) {
            this.t = t;
        }
    }
}
