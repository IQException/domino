package com.ctrip.train.tieyouflight.domino.config;

import org.springframework.cache.interceptor.SimpleKey;

import java.lang.annotation.*;

/**
 * @author wang.wei
 * @since 2019/6/25
 * 各参数的全局默认值见 {@link MultiersCacheProperties}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Domino {

    long UNSET_LONG = Long.MIN_VALUE;

    int UNSET_INT = Integer.MIN_VALUE;

    /**
     * 当需要的数据在缓存中不存在时是否自动加载
     * 注意无论是否自动加载，该任务都将被加入load队列（refreshable=true)：如果不希望等待加载，可以设置为false
     *
     * @return
     */
    boolean autoLoad() default true;

    /**
     * 使用本地缓存
     *
     * @return
     */
    boolean useLocalCache() default true;

    /**
     * @return
     */
    String localCacheManager() default "";

    /**
     * @return
     */
    String remoteCacheManager() default "";

    /**
     * 使用分布式缓存
     *
     * @return
     */
    boolean useRemoteCache() default true;

    /**
     * 是否需要定时刷新
     *
     * @return
     */
    boolean refreshable() default false;

    /**
     * 数据刷新的间隔时间(s)
     * 当数据的超时时间小于数据的刷新间隔时，数据永不过时，同时能保证一定的时效性，建议按这种方式设置
     *
     * @return
     */
    long interval() default UNSET_LONG;

    /**
     * 刷新的超时时间(s): 默认为interval * 2
     * 如果某个key超过此时间无请求，则踢出刷新队列
     *
     * @return
     */
    long refreshTimeout() default UNSET_LONG;

    /**
     * 序列化和反序列化器
     *
     * @return
     */
    String serializer() default "";

    /**
     * refresh失败重试次数
     * 失败情况： 1. cacheNullValues==false && result==null ; 2. cacheEmptyValues==false && result is empty ; 3. throw exception
     *
     * @return
     */
    int retries() default UNSET_INT;

    /**
     * 启动时加载数据，只支持无参方法
     *
     * @return
     */
    boolean loadOnStartUp() default false;

    /**
     * 是否缓存null
     * 缓存的话有助于提高响应和吞吐，且利用重试机制可以补偿服务抖动的情况，但是数据会比较老；
     * 不缓存的话不用补偿重试，但是对于不稳定的服务或者响应比较慢的服务会影响性能（配合Cacheable（sync=true）会合并大部分请求，消弭性能损耗），数据比较新。
     * <p>
     * 如果对数据的时效性要求不高，而对响应和吞吐要求较高，可以设置为true。
     * 如果希望一旦有数据立即就能展示，可以设置为false。
     *
     * @return
     */
    boolean cacheNullValues() default false;
    /**
     * 是否缓存空Collection/Map/Array；详见cacheNullValues
     *
     * @return
     */
    boolean cacheEmptyValues() default false;

    /**
     * 本地缓存的超时时间(s)
     * 小于0表示永不超时；0表示立即超时（可用于测试）
     *
     * @return
     */
    long localExpireTime() default UNSET_LONG;

    /**
     * 分布式缓存的超时时间(s)
     * 必须大于0
     *
     * @return
     */
    long remoteExpireTime() default UNSET_LONG;

    /**
     * 刷新缓存的线程数
     *
     * @return
     */
    int concurrency() default UNSET_INT;

    /**
     * 本地缓存最大条数:0表示不缓存，可以用作测试
     *
     * @return
     */
    long localSize() default UNSET_LONG;

    /**
     * 刷新数据的延时告警阈值
     * 由于存在队列，数据可能在预期的时间得不到刷新，而是会有一定的延迟；当队列很小（与localSize相关）或者处理速度很快（与concurrency和方法执行时间相关）时，延迟比较小
     * 默认等于interval
     *
     * @return
     */
    long warnDelayTime() default UNSET_LONG;

    /**
     * redis key prefix
     * cacheKey=appid::cacheName::keyPrefix::rawKey
     *
     * @return
     */
    String keyPrefix() default "";

    /**
     * 是否需要合并请求
     * 覆盖@Cacheable的sync设置，因为@Cacheable(sync=true) does not support unless attribute，故另外开辟参数）
     *
     * @return
     */
    boolean sync() default true;

    /**
     * key的类型，用来反序列化
     *
     * @return
     */
    Class keyType() default SimpleKey.class;

}
