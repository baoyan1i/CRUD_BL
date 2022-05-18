package io.simpleframework.crud.exception;

import lombok.Getter;

import java.io.Serializable;


@Getter
public class DomainNotFoundException extends RuntimeException {

    private final Class<?> clazz;
    private final Serializable id;

    public DomainNotFoundException(Class<?> clazz, Serializable id) {
        super("Domain not found from " + clazz + ": " + id);
        this.clazz = clazz;
        this.id = id;
    }

}
