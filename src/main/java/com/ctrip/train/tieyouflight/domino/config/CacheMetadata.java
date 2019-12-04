package com.ctrip.train.tieyouflight.domino.config;

import java.lang.reflect.Type;

/**
 * @author wang.wei
 * @since 2019/6/26
 */
public class CacheMetadata {

    private CacheConfig cacheConfig;

    private Type keyType;

    private Type valueType;

    private Type[] methodParamTypes;

    private Object bean;

    private String methodName;

    public Object getBean() {
        return bean;
    }

    public CacheMetadata setBean(Object bean) {
        this.bean = bean;
        return this;
    }

    public Type getKeyType() {
        return keyType;
    }

    public CacheMetadata setKeyType(Type keyType) {
        this.keyType = keyType;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public CacheMetadata setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public CacheMetadata setCacheConfig(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
        return this;
    }

    public Type getValueType() {
        return valueType;
    }

    public CacheMetadata setValueType(Type valueType) {
        this.valueType = valueType;
        return this;
    }

    public Type[] getMethodParamTypes() {
        return methodParamTypes;
    }

    public CacheMetadata setMethodParamTypes(Type[] methodParamTypes) {
        this.methodParamTypes = methodParamTypes;
        return this;
    }
}
