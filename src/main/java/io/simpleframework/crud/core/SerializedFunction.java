package io.simpleframework.crud.core;

import java.io.Serializable;


@FunctionalInterface
public interface SerializedFunction<T, R> extends Serializable {

    
    R apply(T t);

}
