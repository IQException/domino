package com.ctrip.train.tieyouflight.domino.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author wang.wei
 * @since 2019/7/5
 */
public class DefaultKeySerializer extends StdSerializer {

    private JacksonSerializer jacksonSerializer = JacksonSerializer.DEFAULT;

    protected DefaultKeySerializer(Class t) {
        super(t);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
//        StringWriter writer = new StringWriter();
//        jacksonSerializer.getMapper().writeValue(writer, value);
        gen.writeFieldName(jacksonSerializer.serialize(value));
    }
}
