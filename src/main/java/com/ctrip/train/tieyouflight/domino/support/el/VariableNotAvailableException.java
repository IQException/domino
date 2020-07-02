package com.ctrip.train.tieyouflight.domino.support.el;

import org.springframework.expression.EvaluationException;

/**
 * @author wang.wei
 * @since 2020/4/21
 */
public class VariableNotAvailableException extends EvaluationException {
    private final String name;

    public VariableNotAvailableException(String name) {
        super("Variable '" + name + "' is not available");
        this.name = name;
    }


    public String getName() {
        return this.name;
    }
}
