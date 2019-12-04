package com.ctrip.train.tieyouflight.domino.support.load;

import com.ctrip.train.tieyouflight.domino.support.load.LoadingHolder;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.ExecutionException;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class FutureLoadingHolder<K, V> implements LoadingHolder<K, V> {
    private volatile SettableFuture<LoadingEntry<K, V>> futureValue = SettableFuture.create();
    private volatile boolean loading;
//	private volatile LoadingEntry<V> oldValue;

    @Override
    public boolean isLoading() {
        return this.loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
//		if(loading) {
//			this.futureValue = SettableFuture.create();
//		}else{
//			this.futureValue = null;
//		}
    }

    public boolean set(LoadingEntry<K, V> newValue) {
        return futureValue.set(newValue);
    }

    public boolean setException(Throwable t) {
        return futureValue.setException(t);
    }

    @Override
    public void notifyNewValue(LoadingEntry<K, V> entry) {

        set(entry);
    }

    @Override
    public LoadingEntry<K, V> waitForValue() throws ExecutionException {
        return Uninterruptibles.getUninterruptibly(futureValue);
    }

    /**
     * @see LoadingHolder#recreateFutureTask()
     */
    @Override
    public void recreateFutureTask() {
        this.futureValue = SettableFuture.create();
    }

//	@Override
//	public LoadingEntry<V> get() {
//		return oldValue;
//	}
}
