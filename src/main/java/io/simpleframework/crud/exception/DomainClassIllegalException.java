package io.simpleframework.crud.exception;

import lombok.Getter;


@Getter
public class DomainClassIllegalException extends RuntimeException {

    private final Class<?> clazz;

    public DomainClassIllegalException(Class<?> clazz, String msg) {
        super(msg);
        this.clazz = clazz;
    }

    public DomainClassIllegalException(Class<?> clazz, Exception e) {
        super(e);
        this.clazz = clazz;
    }

}
