package io.simpleframework.crud;

import io.simpleframework.crud.core.ModelConfiguration;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.List;


public interface ModelInfo<T> extends Serializable {

    
    Class<T> modelClass();

    
    @SneakyThrows
    default T newModelInstance() {
        return this.modelClass().newInstance();
    }

    
    default boolean isBaseModel() {
        return BaseModel.class.isAssignableFrom(this.modelClass());
    }

    
    default BaseModelMapper<T> mapper() {
        return this.mapper(this.modelClass());
    }

    
    <R extends T> BaseModelMapper<R> mapper(Class<R> clazz);

    
    ModelConfiguration config();

    
    String name();

    
    ModelField id();

    
    List<ModelField> getAllFields();

    
    List<ModelField> getInsertFields();

    
    List<ModelField> getUpdateFields();

    
    ModelField getField(String fieldName);

}
