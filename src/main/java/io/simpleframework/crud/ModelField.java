package io.simpleframework.crud;

import java.io.Serializable;


public interface ModelField extends Serializable {

    
    <T> Object getValue(T model);

    
    <T> void setValue(T model, Object value);

    
    String column();

    
    String fieldName();

    
    Class<?> fieldType();

    
    boolean insertable();

    
    boolean updatable();

}
