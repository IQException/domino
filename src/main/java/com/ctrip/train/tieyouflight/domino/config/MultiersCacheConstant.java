package com.ctrip.train.tieyouflight.domino.config;

/**
 * @author wang.wei
 * @since 2019/5/14
 */
public class MultiersCacheConstant {

    public static final String FIRST_TIER_CACHE_MANAGER = "t1CacheManager";

    public static final String SECOND_TIER_CACHE_MANAGER = "t2CacheManager";

    public static final int FIRST_TIER_CACHE_TTL = 300;

    public static final int SECOND_TIER_CACHE_TTL = 3600 * 2;

    public static final int CAFFEINE_MAX_ENTRY_SIZE = 300;


}
