package com.ctrip.train.tieyouflight.domino.support;

import com.ctrip.train.tieyouflight.domino.TieredCache;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractTieredCache implements TieredCache {

    private final boolean allowNullValues;

    private final boolean allowEmptyValues;


    /**
     * Create an {@code AbstractTieredCache} with the given setting.
     *
     * @param allowNullValues whether to allow for {@code null} values
     */
    protected AbstractTieredCache(boolean allowNullValues, boolean allowEmptyValues) {
        this.allowNullValues = allowNullValues;
        this.allowEmptyValues = allowEmptyValues;
    }

    /**
     * Return whether {@code null} values are allowed in this cache.
     */
    public final boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    public boolean isAllowEmptyValues() {
        return allowEmptyValues;
    }

    /**
     * @param value can be {@literal null}.
     * @return to be stored value. Can be {@literal null}.
     */
    @Nullable
    protected Object toStoreValue(@Nullable Object value) {
        if (this.allowNullValues && value == null) {
            return NullValue.INSTANCE;
        }
        return value;
    }



}