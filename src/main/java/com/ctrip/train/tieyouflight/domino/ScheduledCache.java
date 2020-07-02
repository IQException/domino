package com.ctrip.train.tieyouflight.domino;

import org.springframework.cache.Cache;

/**
 * @author wang.wei
 * @since 2020/4/22
 */
public interface ScheduledCache extends Cache {

    /**
     * 后台刷新缓存
     * @param key
     * @param value
     */
    boolean refresh(Object key, Object value);

    /**
     * 刷新更新时间
     * @param key
     */
    void refreshUpdateTime(Object key);


}
