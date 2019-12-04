package com.ctrip.train.tieyouflight.domino.support;

import java.io.Serializable;

/**
 * Simple serializable class that serves as a {@code null} replacement
 * for cache stores which otherwise do not support {@code null} values.
 *
 * @author Juergen Hoeller
 * @see AbstractTieredCache
 * @since 4.2.2
 */
public final class NullValue implements Serializable {

    public static final Object INSTANCE = new NullValue();

    private static final long serialVersionUID = 1L;


    private NullValue() {
    }

    private Object readResolve() {
        return INSTANCE;
    }

}