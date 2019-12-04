package com.ctrip.train.tieyouflight.domino.support.load;

import com.ctrip.train.tieyouflight.domino.support.load.LoadingContext;
import com.ctrip.train.tieyouflight.domino.support.load.MergedKey;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class ObjectMergedKey implements MergedKey<Object> {
    private Object key;

    public ObjectMergedKey(Object key) {
        this.key = key;
    }

    @Override
    public int hash(LoadingContext context) {
        return this.key.hashCode();
    }

    @Override
    public boolean equalsKey(Object other, LoadingContext context) {
        return this.key.equals(other);
    }

    /**
     * @see MergedKey#getKey()
     */
    @Override
    public Object getKey() {
        return this.key;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.key.toString();
    }

}

