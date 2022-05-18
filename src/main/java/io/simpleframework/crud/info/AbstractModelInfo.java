package io.simpleframework.crud.info;

import io.simpleframework.crud.BaseModelMapper;
import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.core.DatasourceType;
import io.simpleframework.crud.core.ModelConfiguration;
import io.simpleframework.crud.mapper.mybatis.MybatisModelMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@SuppressWarnings("all")
public abstract class AbstractModelInfo<T> implements ModelInfo<T> {

    
    private final Map<Class, BaseModelMapper> MAPPERS = new ConcurrentHashMap<>();
    
    private final Class modelClass;
    
    private final ModelConfiguration config;
    
    private final String modelName;
    
    private ModelId id;
    
    private final Map<String, ModelField> fields;

    protected AbstractModelInfo(Class modelClass, ModelConfiguration config, String modelName) {
        this.modelClass = modelClass;
        this.config = config;
        this.modelName = modelName;
        this.fields = new LinkedHashMap<>();
        MAPPERS.put(modelClass, this.createMapper(modelClass));
    }

    @Override
    public Class<T> modelClass() {
        return this.modelClass;
    }

    @Override
    public <R extends T> BaseModelMapper<R> mapper(Class<R> clazz) {
        return MAPPERS.computeIfAbsent(clazz, k -> this.createMapper(clazz));
    }

    @Override
    public ModelConfiguration config() {
        return this.config;
    }

    @Override
    public String name() {
        return this.modelName;
    }

    @Override
    public ModelId id() {
        return this.id;
    }

    @Override
    public List<ModelField> getAllFields() {
        return new ArrayList<>(this.fields.values());
    }

    @Override
    public List<ModelField> getInsertFields() {
        return this.getAllFields()
                .stream()
                .filter(ModelField::insertable)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelField> getUpdateFields() {
        return this.getAllFields()
                .stream()
                .filter(ModelField::updatable)
                .collect(Collectors.toList());
    }

    @Override
    public ModelField getField(String fieldName) {
        return this.fields.get(fieldName);
    }

    protected void setId(ModelId id) {
        this.id = id;
    }

    protected Map<String, ModelField> fields() {
        return this.fields;
    }

    protected void addField(List<ModelField> fields) {
        for (ModelField field : fields) {
            this.addField(field);
        }
    }

    protected void addField(ModelField field) {
        this.fields.put(field.fieldName(), field);
    }

    
    protected BaseModelMapper<?> createMapper(Class clazz) {
        DatasourceType datasourceType = this.config.datasourceType();
        if (datasourceType == DatasourceType.Mybatis) {
            return new MybatisModelMapper(clazz, this);
        } else {
            throw new IllegalArgumentException("DatasourceType is not support " + datasourceType);
        }
    }

}
