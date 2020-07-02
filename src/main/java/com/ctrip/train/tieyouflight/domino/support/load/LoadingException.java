package com.ctrip.train.tieyouflight.domino.support.load;

/**
 * @author wang.wei
 * @since 2019/5/16
 */
public class LoadingException extends RuntimeException{

    public LoadingException(String message) {
        super(message);
    }
    /** TODO description here
     *
     */
    public LoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}

