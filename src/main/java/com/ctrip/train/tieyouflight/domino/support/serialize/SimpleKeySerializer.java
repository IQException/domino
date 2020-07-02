package com.ctrip.train.tieyouflight.domino.support.serialize;

import org.springframework.cache.interceptor.SimpleKey;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * @author wang.wei
 * @since 2020/4/8
 */
public class SimpleKeySerializer implements KeySerializer {
    private static final SimpleKeySerializer INSTANCE = new SimpleKeySerializer();
    private static final JacksonSerializer jsonSerializer = JacksonSerializer.DEFAULT;

    public static SimpleKeySerializer getInstance() {
        return INSTANCE;
    }

    @Override
    public String serializeKey(Object o) {
        try {
            SimpleKey simpleKey = (SimpleKey) o;
            Field paramsField = SimpleKey.class.getDeclaredField("params");
            paramsField.setAccessible(true);
            Object[] params = (Object[]) paramsField.get(simpleKey);
            String[] cacheParams = new String[params.length];
            int i = 0;
            for (Object param : params) {
                cacheParams[i++] = jsonSerializer.serialize(param);
            }
            return jsonSerializer.serialize(cacheParams);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Object deserializeKey(String source, Serializer.Context context) {
        String[] paramsStr = jsonSerializer.deserialize(source, String[].class);
        Object[] params = new Object[paramsStr.length];
        Type[] paramTypes = context.getParamTypes();
        int i = 0;
        for (String paramStr : paramsStr) {
            params[i] = jsonSerializer.deserialize(paramStr, SerializeUtil.getJavaType(paramTypes[i]));
            i++;
        }
        if (params.length > 0)
            return new SimpleKey(params);
        else
            return SimpleKey.EMPTY;
    }
}
