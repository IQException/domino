package com.ctrip.train.tieyouflight.domino.support.serialize;

/**
 * @author wang.wei
 * @since 2020/4/8
 */
public interface KeySerializer {
    String serializeKey(Object key);

    Object deserializeKey(String source, Serializer.Context context);

}
