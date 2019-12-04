package com.ctrip.train.tieyouflight.domino.config;

import com.ctrip.framework.foundation.Foundation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author wang.wei
 * @since 2019/5/15
 */
@Configuration
@ConfigurationProperties(prefix = "domino")
public class MultiersCacheProperties {


    private long interval = 5 * 60;

    private long refreshTimeout = 5 * 60 * 2;

    private int retries = 1;

    private long localExpireTime = 30 * 60;

    private long remoteExpireTime = 3 * 3600;

    private int concurrency = 1;

    private long localSize = 1000;

    private long warnDelayTime = 5 * 60;

    public long getInterval() {
        return interval;
    }

    public MultiersCacheProperties setInterval(long interval) {
        this.interval = interval;
        return this;
    }

    public long getRefreshTimeout() {
        return refreshTimeout;
    }

    public MultiersCacheProperties setRefreshTimeout(long refreshTimeout) {
        this.refreshTimeout = refreshTimeout;
        return this;
    }

    public long getLocalExpireTime() {
        return localExpireTime;
    }

    public MultiersCacheProperties setLocalExpireTime(long localExpireTime) {
        this.localExpireTime = localExpireTime;
        return this;
    }

    public long getRemoteExpireTime() {
        return remoteExpireTime;
    }

    public MultiersCacheProperties setRemoteExpireTime(long remoteExpireTime) {
        this.remoteExpireTime = remoteExpireTime;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public MultiersCacheProperties setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public MultiersCacheProperties setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public long getLocalSize() {
        return localSize;
    }

    public MultiersCacheProperties setLocalSize(long localSize) {
        this.localSize = localSize;
        return this;
    }

    public long getWarnDelayTime() {
        return warnDelayTime;
    }

    public MultiersCacheProperties setWarnDelayTime(long warnDelayTime) {
        this.warnDelayTime = warnDelayTime;
        return this;
    }

    private final Caffeine caffeine = new Caffeine();

    private final Redis redis = new Redis();

    public Redis getRedis() {
        return redis;
    }

    public Caffeine getCaffeine() {
        return caffeine;
    }


    /**
     * Caffeine specific cache properties.
     */
    public static class Caffeine {

        /**
         * The spec to use to create caches. Check CaffeineSpec for more details on the
         * spec format.
         */
        private String spec;

        public String getSpec() {
            return this.spec;
        }

        public void setSpec(String spec) {
            this.spec = spec;
        }

    }


    /**
     * Redis specific cache properties.
     */
    public static class Redis {


        private String keyPrefix = Foundation.app().getAppId();

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public Redis setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }
    }
}
