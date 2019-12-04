package com.ctrip.train.tieyouflight.domino.autoconfigure;

import com.ctrip.train.tieyouflight.domino.CRedisCacheManager;
import com.ctrip.train.tieyouflight.domino.CaffeineCacheManager;
import com.ctrip.train.tieyouflight.domino.MultiersCacheManager;
import com.ctrip.train.tieyouflight.domino.TieredCacheManager;
import com.ctrip.train.tieyouflight.domino.config.MultiersCacheProperties;
import com.ctrip.train.tieyouflight.domino.support.SpringCacheManager;
import credis.java.client.CacheProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
@EnableCaching
@Configuration
public class MultiersCacheConfiguration {

    private MultiersCacheProperties multiersCacheProperties;

    private TieredCacheManager localCacheManager;

    private TieredCacheManager remoteCacheManager;

    private CacheManager springCacheManager;

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      TieredCacheManager localCacheManager,
                                      CacheProvider cacheProvider,
                                      CacheManager springCacheManager) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = new CRedisCacheManager(cacheProvider);
        this.springCacheManager = springCacheManager;
    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      TieredCacheManager localCacheManager,
                                      CacheProvider cacheProvider) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = new CRedisCacheManager(cacheProvider);
    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      CacheProvider cacheProvider) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = new CaffeineCacheManager();
        this.remoteCacheManager = new CRedisCacheManager(cacheProvider);
    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      CacheProvider cacheProvider,
                                      SpringCacheManager springCacheManager) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = new CaffeineCacheManager();
        this.remoteCacheManager = new CRedisCacheManager(cacheProvider);
        this.springCacheManager = springCacheManager.cacheManager();
    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      TieredCacheManager localCacheManager,
                                      TieredCacheManager remoteCacheManager,
                                      SpringCacheManager springCacheManager) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = localCacheManager;
        this.remoteCacheManager = remoteCacheManager;
        this.springCacheManager = springCacheManager.cacheManager();

    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      TieredCacheManager remoteCacheManager) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = new CaffeineCacheManager();
        this.remoteCacheManager = remoteCacheManager;
    }

    @Autowired(required = false)
    public MultiersCacheConfiguration(MultiersCacheProperties multiersCacheProperties,
                                      TieredCacheManager remoteCacheManager,
                                      SpringCacheManager springCacheManager) {
        this.multiersCacheProperties = multiersCacheProperties;
        this.localCacheManager = new CaffeineCacheManager();
        this.remoteCacheManager = remoteCacheManager;
        this.springCacheManager = springCacheManager.cacheManager();
    }

    @Bean
    public MultiersCacheManager cacheManager() {
        return new MultiersCacheManager(multiersCacheProperties, localCacheManager, remoteCacheManager, springCacheManager);
    }
}
