package com.ctrip.train.tieyouflight.domino.support;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author wang.wei
 * @since 2019/8/11
 */
public class CacheUtil {
    public static boolean isEmpty(Object value) {
        return value == null
                || (value.getClass().isArray() && ArrayUtils.getLength(value) == 0)
                || (Collection.class.isAssignableFrom(value.getClass()) && CollectionUtils.isEmpty((Collection) value))
                || (Map.class.isAssignableFrom(value.getClass()) && MapUtils.isEmpty((Map) value));
    }
}
