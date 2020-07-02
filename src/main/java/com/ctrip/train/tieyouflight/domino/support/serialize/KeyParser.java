package com.ctrip.train.tieyouflight.domino.support.serialize;

import java.lang.reflect.Method;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public interface KeyParser {

    Object[] parse(Method method, Object key);
}
