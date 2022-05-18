package io.simpleframework.crud.info.dynamic;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.annotation.IdStrategy;
import io.simpleframework.crud.core.DatasourceType;
import io.simpleframework.crud.core.IdType;
import io.simpleframework.crud.core.ModelConfiguration;
import io.simpleframework.crud.info.AbstractModelInfo;
import io.simpleframework.crud.info.ModelId;
import io.simpleframework.crud.util.SimpleCrudUtils;

import java.util.Map;


public class DynamicModelInfo extends AbstractModelInfo<Map<String, Object>> {

    public DynamicModelInfo(String modelName, DatasourceType datasourceType) {
        this(modelName, datasourceType, "");
    }

    public DynamicModelInfo(String modelName, DatasourceType datasourceType, String datasourceName) {
        super(Map.class, new ModelConfiguration(datasourceType, datasourceName), modelName);
    }

    public DynamicModelInfo setId(String fieldName) {
        return this.setId(fieldName, Long.class);
    }

    public DynamicModelInfo setId(String fieldName, String column) {
        return this.setId(fieldName, column, Long.class, IdType.SNOWFLAKE);
    }

    public DynamicModelInfo setId(String fieldName, Class<?> fieldType) {
        return this.setId(fieldName, fieldType, IdType.SNOWFLAKE);
    }

    public DynamicModelInfo setId(String fieldName, Class<?> fieldType, IdType idType) {
        String column = SimpleCrudUtils.camelToUnderline(fieldName);
        return this.setId(fieldName, column, fieldType, idType);
    }

    public DynamicModelInfo setId(String fieldName, String column, Class<?> fieldType, IdType idType) {
        return this.setId(fieldName, column, fieldType, idType, IdStrategy.SNOWFLAKE_BEGIN_TIME);
    }

    public DynamicModelInfo setId(String fieldName, String column, Class<?> fieldType, IdType type, long beginTime) {
        if (SimpleCrudUtils.isBlank(column)) {
            column = SimpleCrudUtils.camelToUnderline(fieldName);
        }
        ModelField field = new DynamicModelField(fieldName, column, fieldType);
        super.setId(new ModelId(field, type, beginTime));
        super.addField(field);
        return this;
    }

    public DynamicModelInfo addField(String fieldName) {
        return this.addField(fieldName, String.class);
    }

    public DynamicModelInfo addField(String fieldName, String column) {
        return this.addField(fieldName, column, String.class);
    }

    public DynamicModelInfo addField(String fieldName, Class<?> fieldType) {
        String column = SimpleCrudUtils.camelToUnderline(fieldName);
        return this.addField(fieldName, column, fieldType);
    }

    public DynamicModelInfo addField(String fieldName, String column, Class<?> fieldType) {
        if (this.isIdField(fieldName)) {
            throw new IllegalArgumentException("field [" + fieldName + "] is primary key.");
        }
        if (SimpleCrudUtils.isBlank(column)) {
            column = SimpleCrudUtils.camelToUnderline(fieldName);
        }
        ModelField field = new DynamicModelField(fieldName, column, fieldType);
        super.addField(field);
        return this;
    }

    public DynamicModelInfo removeField(String fieldName) {
        if (fieldName == null) {
            return this;
        }
        if (this.isIdField(fieldName)) {
            super.setId(null);
        }
        super.fields().remove(fieldName);
        return this;
    }

    public DynamicModelInfo removeAllFields() {
        super.setId(null);
        super.fields().clear();
        return this;
    }

    private boolean isIdField(String fieldName) {
        ModelField modelId = super.id();
        if (modelId == null) {
            return false;
        }
        return modelId.fieldName().equals(fieldName);
    }

}
