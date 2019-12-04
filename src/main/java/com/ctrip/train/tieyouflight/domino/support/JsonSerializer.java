package com.ctrip.train.tieyouflight.domino.support;

import com.fasterxml.jackson.databind.JavaType;
import org.apache.commons.io.Charsets;
import org.springframework.cache.interceptor.SimpleKey;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author wang.wei
 * @since 2019/6/26
 */
public class JsonSerializer implements Serializer {

    private static final JsonSerializer INSTANCE = new JsonSerializer();

    private static final JacksonSerializer jsonSerializer = JacksonSerializer.DEFAULT;

    public static JsonSerializer getInstance() {
        return INSTANCE;
    }


    @Override
    public String serializeKey(Object o) {
        if (o.getClass() == SimpleKey.class)
            return serializeSimpleKey(o);
        return jsonSerializer.serialize(o);
    }

    private String serializeSimpleKey(Object o) {
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
    public Object deserializeKey(String source, Context context) {
        Type keyType = context.getKeyType();
        if (keyType == SimpleKey.class) {
            return deserializeSimpleKey(source, context);
        }
        JavaType javaType = SerializeUtil.getJavaType(keyType);
        return jsonSerializer.deserialize(source, javaType);

    }

    private Object deserializeSimpleKey(String source, Context context) {
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

    @Override
    public byte[] serializeValue(Object o, Context context) {
        if (context.getValueType() instanceof ParameterizedType &&
                Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) context.getValueType()).getRawType())) {
            Class keyType = getKeyType(context.getValueType());
            jsonSerializer.addKeySerializer(keyType, new DefaultKeySerializer(keyType));
        }
        return jsonSerializer.serialize(o).getBytes(Charsets.UTF_8);
    }

    @Override
    public Object deserializeValue(byte[] source, Context context) {
        if (context.getValueType() instanceof ParameterizedType &&
                Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) context.getValueType()).getRawType())) {
            Class keyType = getKeyType(context.getValueType());
            jsonSerializer.addKeyDeserializer(keyType, new DefaultKeyDeserializer(keyType));
        }
        JavaType javaType = SerializeUtil.getJavaType(context.getValueType());
        return jsonSerializer.deserialize(new String(source, Charsets.UTF_8), javaType);
    }

    private Class getKeyType(Type mapType) {
        ParameterizedType parameterizedType = (ParameterizedType) mapType;
        Type[] argTypes = parameterizedType.getActualTypeArguments();
        if (argTypes == null || argTypes.length == 0) {
            throw new RuntimeException("can't serialize the map that has no key or value types!");
        }
        Type keyType = argTypes[0];
        if (!(keyType instanceof Class)) {
            throw new RuntimeException("only support return map that key type is raw type!");
        }
        return (Class) keyType;
    }
}
