package com.ctrip.train.tieyouflight.domino.config;

import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Method;
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

    private Method targetMethod;

    private Class targetClass;

    private BeanFactory beanFactory;

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public CacheMetadata setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        return this;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public CacheMetadata setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
        return this;
    }

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
        return targetMethod.getName();
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public CacheMetadata setTargetMethod(Method targetMethod) {
        this.targetMethod = targetMethod;
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
