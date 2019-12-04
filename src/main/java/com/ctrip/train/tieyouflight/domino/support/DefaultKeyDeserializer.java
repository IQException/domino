package com.ctrip.train.tieyouflight.domino.support;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

/**
 * @author wang.wei
 * @since 2019/7/5
 */
public class DefaultKeyDeserializer extends KeyDeserializer {
    private Class keyType;

    public DefaultKeyDeserializer(Class keyType) {
        this.keyType = keyType;
    }

    private JacksonSerializer jacksonSerializer = JacksonSerializer.DEFAULT;

    @Override
    public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
        return jacksonSerializer.deserialize(key, keyType);
    }
}
