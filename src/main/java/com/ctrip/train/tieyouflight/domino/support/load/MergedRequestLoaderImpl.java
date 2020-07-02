package com.ctrip.train.tieyouflight.domino.support.load;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.support.serialize.JsonSerializer;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class MergedRequestLoaderImpl<V> implements MergedRequestLoader<V> {
    private MergeRequestLoaderCallback<V> callback;
    private AtomicReferenceArray<LoadingHolder<Object, V>> loadingHolderArray;
    private static final int DEFAULT_HOLDER_ARRAY_SIZE = 1024 * 4;
    private static final String LOG_TITLE = "MergeRequestLoader";

    public MergedRequestLoaderImpl(MergeRequestLoaderCallback<V> callback) {
        this(callback, DEFAULT_HOLDER_ARRAY_SIZE);
    }

    public MergedRequestLoaderImpl(MergeRequestLoaderCallback<V> callback,
                                   int size) {
        this.callback = callback;
        this.loadingHolderArray = new AtomicReferenceArray<LoadingHolder<Object, V>>(
                size);
    }

    static int rehash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        // TODO(kevinb): use Hashing/move this to Hashing?
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);

        // return
        // hash(MurmurHash.hash64A(EncodeUtils.encode(String.valueOf(h)),0x1234ABCD));
    }

    @Override
    public V load(Object key, LoadingContext<V> context) {
        this.callback.prepare(key, context);
        Map<String, String> logTags = Maps.newHashMap();
        logTags.put("key", JsonSerializer.getInstance().serializeKey(key));
        try {
            int hash = rehash(key.hashCode());
            int index = hash & (loadingHolderArray.length() - 1);
            // logger.debug("Genereate reference reference key: {0}, index: {1}, hash:{2}",
            // key, index, hash);
            LoadingHolder<Object, V> reference = loadingHolderArray.get(index);
            if (reference == null) {
                synchronized (loadingHolderArray) {
                    reference = loadingHolderArray.get(index);
                    if (reference == null) {
                        ContextAwareClogger.info(LOG_TITLE,
                                String.format("Generate a loading holder for key:%s, index:%s",
                                        key, index), logTags);
                        reference = new FutureLoadingHolder<Object, V>();
                        loadingHolderArray.set(index, reference);
                    }
                }
            }

            V result = null;
            if (reference.isLoading()) {
                try {
                    LoadingHolder.LoadingEntry<Object, V> entry = reference.waitForValue();
                    ContextAwareClogger.info(LOG_TITLE,
                            String.format("Thread: %s waited the result for key: %s, confilict with key: %s, index: %s",
                                    Thread.currentThread().getName(), key,
                                    entry.getKey(), index), logTags);
                    if (!entry.getMergedKeyValue().equals(key)) {
                        // compare and set
                        ContextAwareClogger.warn(LOG_TITLE,
                                String.format("Reload again! Thread: %s, Hash value of key: " +
                                                "{0} not match with the loading holder key:%s, hash:%s, index: %s.",
                                        Thread.currentThread().getName(), entry.getKey(), key, hash, index), logTags);
                        result = load(key, context);
                    } else {
                        result = entry.getValue();
                        if (result == null) {
                            Throwable t = entry.getT();
                            if (t != null) {
                                throw new LoadingException(t.getMessage(), t);
                            }

                            ContextAwareClogger.info(LOG_TITLE,
                                    String.format("Result value of key: %s, hash:%s, index: %s is null",
                                            key, hash, index), logTags);
                        }
                    }
                } catch (ExecutionException e) {
                    throw new LoadingException(e.getMessage(), e);
                }
            } else {
                synchronized (reference) {
                    result = this.callback.get(key, context);
                    if (result == null) {
                        LoadingHolder.LoadingEntry<Object, V> entry = new LoadingHolder.LoadingEntry<Object, V>(new ObjectMergedKey(key));
                        V object = null;
                        try {
                            reference.setLoading(true);
                            object = this.callback.load(key, context);
                            ContextAwareClogger.info(LOG_TITLE, String.format("Thread: %s loaded key:%s", Thread
                                    .currentThread().getName(), key), logTags);
                            entry.setValue(object);
                            result = object;
                        } catch (Throwable t) {
                            entry.setT(t);
                            throw new LoadingException(t.getMessage(), t);
                        } finally {
                            reference.notifyNewValue(entry);
                            reference.setLoading(false);
                            if (result == null) {
                                ContextAwareClogger.info(LOG_TITLE,
                                        String.format("Result value of key: %s, hash:%s, index: %s is null",
                                                key, hash, index), logTags);
                            }
                            // recreate future task
                            reference.recreateFutureTask();
                        }
                    }
                }
            }
            return result;
        } finally {
            this.callback.postAfter(key, context);
        }
    }

    @Override
    public boolean isRunning(Object key) {
        int hash = rehash(key.hashCode());
        int index = hash & (loadingHolderArray.length() - 1);
        LoadingHolder<Object, V> reference = loadingHolderArray.get(index);
        if (reference == null) {
            return false;
        } else {
            return reference.isLoading();
        }
    }
}
