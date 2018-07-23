package com.fclassroom.ssp.spring.cache.interceptor;

import org.springframework.expression.EvaluationException;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午5:15
 */
public class MSVariableNotAvailableException extends EvaluationException {

    private final String name;

    public MSVariableNotAvailableException(String name) {
        super("Variable '" + name + "' is not available");
        this.name = name;
    }


    public String getName() {
        return this.name;
    }

}
