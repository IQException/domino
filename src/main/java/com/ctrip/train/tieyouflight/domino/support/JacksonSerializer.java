package com.ctrip.train.tieyouflight.domino.support;

import com.ctrip.soa.caravan.common.serializer.*;
import com.ctrip.soa.caravan.common.value.CollectionValues;
import com.ctrip.soa.caravan.common.value.XMLValues;
import com.ctrip.soa.caravan.util.serializer.ssjson.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wang.wei
 * @since 2019/7/5
 */
public class JacksonSerializer implements StreamSerializer, StringSerializer, TypeReferenceDeserializer, JavaTypeDeserializer {
    public static final JacksonSerializer DEFAULT = new JacksonSerializer();

    private ObjectMapper _mapper = new ObjectMapper();
    private SimpleModule module;
    private SSJsonSerializerConfig _config;
    private Logger logger = LoggerFactory.getLogger(JacksonSerializer.class);
    private final ConcurrentMap<Class, JsonSerializer> keySerializerMap = Maps.newConcurrentMap();
    private final ConcurrentMap<Class, KeyDeserializer> keyDeserializerMap = Maps.newConcurrentMap();


    public JacksonSerializer() {
        this(null);
    }

    public JacksonSerializer(SSJsonSerializerConfig config) {
        _config = config;

        setDefaultConfigValues();
        registerCustomModule();
        configMapper();
    }

    @Override
    public String contentType() {
        return "application/json";
    }

    @Override
    public void serialize(OutputStream os, Object obj) {
        try {
            _mapper.writeValue(os, obj);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    public ObjectMapper getMapper() {
        return _mapper;
    }

    @Override
    public <T> T deserialize(InputStream is, Class<T> clazz) {
        try {
            return _mapper.readValue(is, clazz);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        } finally {
            closeInputStream(is);
        }
    }

    @Override
    public String serialize(Object obj) {
        try {
            return _mapper.writeValueAsString(obj);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @Override
    public <T> T deserialize(String s, Class<T> clazz) {
        try {
            return _mapper.readValue(s, clazz);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @Override
    public <T> T deserialize(String s, JavaType javaType) {
        try {
            return _mapper.readValue(s, javaType);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @Override
    public <T> T deserialize(InputStream is, JavaType javaType) {
        try {
            return _mapper.readValue(is, javaType);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        } finally {
            closeInputStream(is);
        }
    }

    @Override
    public <T> T deserialize(String s, TypeReference<T> typeReference) {
        try {
            return _mapper.readValue(s, typeReference);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        }
    }

    @Override
    public <T> T deserialize(InputStream is, TypeReference<T> typeReference) {
        try {
            return _mapper.readValue(is, typeReference);
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(ex);
        } finally {
            closeInputStream(is);
        }
    }

    private void closeInputStream(InputStream is) {
        if (is != null && _config.isAutoCloseInput()) {
            try {
                is.close();
            } catch (IOException e) {
                logger.warn("Error occurred while closing InputStream.", e);
            }
        }
    }

    private void setDefaultConfigValues() {
        if (_config == null)
            _config = SSJsonSerializerConfig.createDefault();

        if (_config.getCalendarSerializer() == null)
            _config.setCalendarSerializer(SSJsonSerializerConfig.DEFAULT_CALENDAR_SERIALIZER);

        if (CollectionValues.isNullOrEmpty(_config.getCalendarDeserializers()))
            _config.setCalendarDeserializers(new ArrayList<>(SSJsonSerializerConfig.DEFAULT_CALENDAR_DESERIALIZERS));
    }

    private void registerCustomModule() {
        module = new SimpleModule();
        registerAbstractTypeMappings(module);
        registerSerializers(module);

        ModuleConfigurator moduleConfigurator = _config.getModuleConfigurator();
        if (moduleConfigurator != null) {
            moduleConfigurator.configure(module);
        }

        _mapper.registerModule(module);
    }

    private void configMapper() {
        _mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        _mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        _mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        _mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        _mapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        _mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        _mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        _mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        _mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        _mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        _mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        _mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        _mapper.setTimeZone(TimeZone.getDefault());
        _mapper.setLocale(Locale.getDefault(Locale.Category.FORMAT));
        if (null != _config && _config.isIncludeNonNullValues()) {
            _mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        _mapper.findAndRegisterModules();
    }

    private void registerAbstractTypeMappings(SimpleModule module) {
        module.addAbstractTypeMapping(XMLGregorianCalendar.class, XMLValues.xmlGregorianCalendarType());
    }

    private void registerSerializers(SimpleModule module) {
        registerCalendarSerializer(module);
    }

    private void registerCalendarSerializer(SimpleModule module) {
        module.addSerializer(XMLValues.xmlGregorianCalendarType(), new XMLGregorianCalendarSerializer(_config.getCalendarSerializer()));
        module.addDeserializer(XMLValues.xmlGregorianCalendarType(), new XMLGregorianCalendarDeserializer(_config.getCalendarDeserializers()));
    }

    public void addKeySerializer(Class keyType, JsonSerializer keySerializer) {
        JsonSerializer exist = keySerializerMap.putIfAbsent(keyType, keySerializer);
        if (exist != null) return;
        module.addKeySerializer(keyType, keySerializer);
        _mapper.registerModule(module);
    }

    public void addKeyDeserializer(Class keyType, KeyDeserializer keyDeserializer) {
        KeyDeserializer exist = keyDeserializerMap.putIfAbsent(keyType, keyDeserializer);
        if (exist != null) return;
        module.addKeyDeserializer(keyType, keyDeserializer);
        _mapper.registerModule(module);
    }

}
