package com.ctrip.train.tieyouflight.domino.autoconfigure;

import com.ctrip.train.tieyouflight.domino.config.MultiersCacheProperties;
import credis.java.client.CacheProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author wang.wei
 * @since 2019/5/18
 */
@Configuration
@ConditionalOnBean({CacheAspectSupport.class,CacheProvider.class})
@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")
@EnableConfigurationProperties(MultiersCacheProperties.class)
@Import({MultiersCacheConfiguration.class})
public class MultiersCacheAutoConfiguration {
}
