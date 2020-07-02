package com.ctrip.train.tieyouflight.domino;

import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;

import java.util.Collection;

/**
 * @author wang.wei
 * @since 2019/5/15
 */
public interface TieredCacheManager {

    /**
     * Return the cache associated with the given name.
     *
     * @param name the cache identifier (must not be {@code null})
     * @return the associated cache, or {@code null} if none found
     */
    TieredCache getCache(String name, CacheMetadata metadata);


    /**
     * Return a collection of the cache names known by this manager.
     *
     * @return the names of all caches known by the cache manager
     */
    Collection<String> getCacheNames();
}
