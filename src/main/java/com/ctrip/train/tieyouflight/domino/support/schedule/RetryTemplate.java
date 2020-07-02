package com.ctrip.train.tieyouflight.domino.support.schedule;

import com.ctrip.train.tieyouflight.common.log.ContextAwareClogger;

import java.util.concurrent.Callable;

/**
 * @author wang.wei
 * @since 2019/6/27
 */
public class RetryTemplate {
    private static final String LOG_TITLE = "RetryTemplate";

    public static <V> RetryResult<V> tryTodo(Callable<V> callable, int retries) {
        boolean succeed = false;
        V result = null;
        while (retries >= 0 && !succeed) {
            try {
                result = callable.call();
                succeed = true;
            } catch (Exception e) {
                ContextAwareClogger.warn(LOG_TITLE, e);
            } finally {
                retries--;
            }

        }

        return new RetryResult<>(succeed, result);
    }

    public static class RetryResult<T> {
        boolean success;
        T result;

        public RetryResult(boolean success, T result) {
            this.success = success;
            this.result = result;
        }

        public boolean isSuccess() {
            return success;
        }

        public RetryResult<T> setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public T getResult() {
            return result;
        }

        public RetryResult<T> setResult(T result) {
            this.result = result;
            return this;
        }
    }
}
