package com.ctrip.train.tieyouflight.domino;

/**
 * @author wang.wei
 * @since 2019/5/15
 */
public interface TieredCache {

    String getName();

    boolean isCached(Object key);

    Object get(Object key);

    /**
     * Associate the specified value with the specified key in this cache.
     * <p>If the cache previously contained a mapping for this key, the old
     * value is replaced by the specified value.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(Object key, Object value);


    /**
     * Evict the mapping for this key from this cache if it is present.
     *
     * @param key the key whose mapping is to be removed from the cache
     */
    void evict(Object key);

    /**
     * Remove all mappings from the cache.
     */
    void clear();

}
