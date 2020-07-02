package com.ctrip.train.tieyouflight.domino.support;

import com.ctrip.train.tieyouflight.domino.TieredCache;
import com.ctrip.train.tieyouflight.domino.TieredCacheManager;
import com.ctrip.train.tieyouflight.domino.config.CacheMetadata;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Abstract base class implementing the common {@link TieredCacheManager} methods.
 * Useful for 'static' environments where the backing caches do not change.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public abstract class AbstractCacheManager implements TieredCacheManager, InitializingBean {

    private final ConcurrentMap<String, TieredCache> cacheMap = new ConcurrentHashMap<String, TieredCache>(16);

    private volatile Set<String> cacheNames = Collections.emptySet();


    // Early cache initialization on startup

    @Override
    public void afterPropertiesSet() {
        initializeCaches();
    }

    /**
     * Initialize the static configuration of caches.
     * <p>Triggered on startup through {@link #afterPropertiesSet()};
     * can also be called to re-initialize at runtime.
     *
     * @see #loadCaches()
     * @since 4.2.2
     */
    public void initializeCaches() {
        Collection<? extends TieredCache> caches = loadCaches();

        synchronized (this.cacheMap) {
            this.cacheNames = Collections.emptySet();
            this.cacheMap.clear();
            Set<String> cacheNames = new LinkedHashSet<String>(caches.size());
            for (TieredCache cache : caches) {
                String name = cache.getName();
                this.cacheMap.put(name, decorateCache(cache));
                cacheNames.add(name);
            }
            this.cacheNames = Collections.unmodifiableSet(cacheNames);
        }
    }

    /**
     * Load the initial caches for this cache manager.
     * <p>Called by {@link #afterPropertiesSet()} on startup.
     * The returned collection may be empty but must not be {@code null}.
     */
    protected abstract Collection<? extends TieredCache> loadCaches();


    // Lazy cache initialization on access

    @Override
    public TieredCache getCache(String name, CacheMetadata cacheMetadata) {
        TieredCache cache = this.cacheMap.get(name);
        if (cache != null) {
            return cache;
        } else {
            // Fully synchronize now for missing cache creation...
            synchronized (this.cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    cache = getMissingCache(name, cacheMetadata);
                    if (cache != null) {
                        cache = decorateCache(cache);
                        this.cacheMap.put(name, cache);
                        updateCacheNames(name);
                    }
                }
                return cache;
            }
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }


    // Common cache initialization delegates for subclasses

    /**
     * Check for a registered cache of the given name.
     * In contrast to {@link #getCache(String, CacheMetadata)}, this method does not trigger
     * the lazy creation of missing caches via {@link #getMissingCache(String, CacheMetadata)}.
     *
     * @param name the cache identifier (must not be {@code null})
     * @return the associated TieredCache instance, or {@code null} if none found
     * @see #getCache(String, CacheMetadata)
     * @see #getMissingCache(String, CacheMetadata)
     * @since 4.1
     */
    protected final TieredCache lookupCache(String name) {
        return this.cacheMap.get(name);
    }

    /**
     * Dynamically register an additional TieredCache with this manager.
     *
     * @param cache the TieredCache to register
     * @deprecated as of Spring 4.3, in favor of {@link #getMissingCache(String, CacheMetadata)}
     */
    @Deprecated
    protected final void addCache(TieredCache cache) {
        String name = cache.getName();
        synchronized (this.cacheMap) {
            if (this.cacheMap.put(name, decorateCache(cache)) == null) {
                updateCacheNames(name);
            }
        }
    }

    /**
     * Update the exposed {@link #cacheNames} set with the given name.
     * <p>This will always be called within a full {@link #cacheMap} lock
     * and effectively behaves like a {@code CopyOnWriteArraySet} with
     * preserved order but exposed as an unmodifiable reference.
     *
     * @param name the name of the cache to be added
     */
    private void updateCacheNames(String name) {
        Set<String> cacheNames = new LinkedHashSet<String>(this.cacheNames.size() + 1);
        cacheNames.addAll(this.cacheNames);
        cacheNames.add(name);
        this.cacheNames = Collections.unmodifiableSet(cacheNames);
    }


    // Overridable template methods for cache initialization

    /**
     * Decorate the given TieredCache object if necessary.
     *
     * @param cache the TieredCache object to be added to this TieredCacheManager
     * @return the decorated TieredCache object to be used instead,
     * or simply the passed-in TieredCache object by default
     */
    protected TieredCache decorateCache(TieredCache cache) {
        return cache;
    }

    /**
     * Return a missing cache with the specified {@code name} or {@code null} if
     * such cache does not exist or could not be created on the fly.
     * <p>Some caches may be created at runtime if the native provider supports
     * it. If a lookup by name does not yield any result, a subclass gets a chance
     * to register such a cache at runtime. The returned cache will be automatically
     * added to this instance.
     *
     * @param name the name of the cache to retrieve
     * @return the missing cache or {@code null} if no such cache exists or could be
     * created
     * @see #getCache(String, CacheMetadata)
     * @since 4.1
     */
    protected abstract TieredCache getMissingCache(String name, CacheMetadata cacheMetadata);

}

