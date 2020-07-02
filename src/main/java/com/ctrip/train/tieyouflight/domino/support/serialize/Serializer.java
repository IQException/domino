package com.ctrip.train.tieyouflight.domino.support.serialize;

import java.lang.reflect.Type;

/**
 * @author wang.wei
 * @since 2019/6/25
 */
public interface Serializer {
    String serializeKey(Object key);

    Object deserializeKey(String source, Context context);

    byte[] serializeValue(Object value,Context context);

    Object deserializeValue(byte[] source, Context context);

    class Context {
        private Type keyType;
        private Type valueType;
        private Type[] paramTypes;

        public Context(Type keyType, Type valueType, Type[] paramTypes) {
            this.keyType = keyType;
            this.valueType = valueType;
            this.paramTypes = paramTypes;
        }

        public Type getKeyType() {
            return keyType;
        }

        public Type getValueType() {
            return valueType;
        }

        public Type[] getParamTypes() {
            return paramTypes;
        }
    }

}
