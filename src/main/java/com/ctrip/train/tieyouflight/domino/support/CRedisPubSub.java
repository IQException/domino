package com.ctrip.train.tieyouflight.domino.support;

import com.ctrip.framework.foundation.Foundation;
import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;
import com.ctrip.train.tieyouflight.domino.MultiersCacheManager;
import com.google.common.collect.Maps;
import credis.java.client.RedisPubSub;

import java.util.Map;

/**
 * @author wang.wei
 * @since 2019/5/17
 * 利用redis pub/sub 保持缓存一致 ;
 * TODO ：做成cache eventListener, RX处理
 */
public class CRedisPubSub extends RedisPubSub {

    private static final String LOG_TITLE = "CRedisPubSub";

    private MultiersCacheManager cacheManager;

    public CRedisPubSub(MultiersCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    //取得订阅的消息后的处理
    @Override
    public void onMessage(String channel, String message) {
        Map<String, String> logtags = Maps.newHashMap();
        logtags.put("channel", channel);
        logtags.put("message", message);
        try {
            ContextAwareClogger.info(LOG_TITLE, "message received!", logtags);
            cacheManager.evictLocalCache(getCacheName(channel), message);
            cacheManager.refreshUpdateTime(getCacheName(channel), message);
        } catch (Exception e) {
            ContextAwareClogger.error(LOG_TITLE, e, logtags);
        }

    }

    public static String getChannel(String cacheName) {
        return Foundation.app().getAppId() + ":" + cacheName;
    }

    public static String getCacheName(String channel) {
        return channel.substring(Foundation.app().getAppId().length() + 1);
    }

    // 取得按表达式的方式订阅的消息后的处理
    @Override
    public void onPMessage(String pattern, String channel, String message) {
    }

    // 初始化订阅时候的处理
    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        if (subscribedChannels == 1) {
            ContextAwareClogger.info(LOG_TITLE, channel + "  subscribed!");
        }
    }
}
