package com.ctrip.train.tieyouflight.domino.support.serialize;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author wang.wei
 * @since 2020/4/8
 */
public class SimpleKeyParser implements KeyParser {

    private static final SimpleKeyParser INSTANCE = new SimpleKeyParser();

    public static SimpleKeyParser getInstance() {
        return INSTANCE;
    }

    @Override
    public Object[] parse(Method method, Object key) {
        int paramCount = method.getParameterCount();
        if (key.getClass() == SimpleKey.class) {
            if (SimpleKey.EMPTY == key) {
                return new Object[0];
            } else {
                try {
                    Field field = ReflectionUtils.findField(SimpleKey.class, "params");
                    field.setAccessible(true);
                    if (paramCount == 1) {
                        return new Object[]{field.get(key)};
                    } else {
                        return (Object[]) field.get(key);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
        }
        return new Object[]{key};
    }

}
