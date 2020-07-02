package com.ctrip.train.tieyouflight.domino.config;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wang.wei
 * @since 2019/6/25
 * 各参数的全局默认值见 {@link MultiersCacheProperties}
 * 配置优先级 : 注解>qconfig>MultiersCacheProperties
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Cacheable(value = {"toBeOverride"}, sync = true)
public @interface Domino {

    long UNSET_LONG = Long.MIN_VALUE;

    int UNSET_INT = Integer.MIN_VALUE;


    @AliasFor(value = "value", annotation = Cacheable.class)
    String name();

    /**
     * Spring Expression Language (SpEL) expression used for making the method
     * caching conditional.
     * <p>Default is {@code ""}, meaning the method result is always cached.
     * <p>The SpEL expression evaluates against a dedicated context that provides the
     * following meta-data:
     * <ul>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
     * references to the {@link java.lang.reflect.Method method}, target object, and
     * affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
     * ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second argument
     * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
     * can also be accessed by name if that information is available.</li>
     * </ul>
     */
    String cond() default "";

    /**
     * Spring Expression Language (SpEL) expression used to veto method caching.
     * <p>Unlike {@link #cond}, this expression is evaluated after the method
     * has been called and can therefore refer to the {@code result}.
     * <p>Default is {@code ""}, meaning that caching is never vetoed.
     * <p>The SpEL expression evaluates against a dedicated context that provides the
     * following meta-data:
     * <ul>
     * <li>{@code #result} for a reference to the result of the method invocation. For
     * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
     * object, not the wrapper</li>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
     * references to the {@link java.lang.reflect.Method method}, target object, and
     * affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
     * ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second argument
     * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
     * can also be accessed by name if that information is available.</li>
     * </ul>
     *
     * @since 3.2
     */
    String except() default "";

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


}
