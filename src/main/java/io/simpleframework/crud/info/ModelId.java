package io.simpleframework.crud.info;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.annotation.IdStrategy;
import io.simpleframework.crud.core.IdType;
import lombok.SneakyThrows;


public class ModelId extends AbstractModelField {
    private final ModelField field;
    
    private final IdGenerator generator;

    public ModelId(ModelField field) {
        this(field, IdType.SNOWFLAKE, IdStrategy.SNOWFLAKE_BEGIN_TIME);
    }

    public ModelId(ModelField field, IdGenerator idGenerator) {
        this.generator = idGenerator;
        AbstractModelField f = (AbstractModelField) field;
        f.setInsertable(idGenerator != null);
        f.setUpdatable(false);
        this.field = f;
    }

    public ModelId(ModelField field, IdType type, long beginTime) {
        this(field, buildIdGenerator(type, beginTime));

        boolean onlySupportString = type == IdType.UUID32 || type == IdType.UUID36;
        if (onlySupportString && field.fieldType() != String.class) {
            throw new IllegalArgumentException("UUID only support String field.");
        }
    }

    @Override
    public <T> Object getValue(T model) {
        return this.field.getValue(model);
    }

    @Override
    @SneakyThrows
    public <T> void setValue(T model, Object value) {
        if (value != null) {
            this.field.setValue(model, value);
            return;
        }
        if (this.generator == null || this.getValue(model) != null) {
            return;
        }
        value = this.generator.generate();
        if (value != null && String.class.isAssignableFrom(this.field.fieldType())) {
            value = value.toString();
        }
        this.field.setValue(model, value);
    }

    @Override
    public String column() {
        return this.field.column();
    }

    @Override
    public String fieldName() {
        return this.field.fieldName();
    }

    @Override
    public Class<?> fieldType() {
        return this.field.fieldType();
    }

    @Override
    public boolean insertable() {
        return this.field.insertable();
    }

    @Override
    public boolean updatable() {
        return this.field.updatable();
    }

    /**
     * 获取表主键策略
     */
    private static IdGenerator buildIdGenerator(IdType type, long beginTime) {
        if (type == IdType.AUTO_INCREMENT) {
            return null;
        }
        if (type == IdType.UUID32) {
            return IdGenerator.UUID32_ID_GENERATOR;
        }
        if (type == IdType.UUID36) {
            return IdGenerator.UUID36_ID_GENERATOR;
        }
        if (type == IdType.SNOWFLAKE) {
            String beginTimeParam = System.getProperty("simple.snowflake", "");
            if (!"".equals(beginTimeParam)) {
                beginTime = Long.parseLong(beginTimeParam);
            }
            if (beginTime <= 0 || beginTime == IdStrategy.SNOWFLAKE_BEGIN_TIME) {
                return IdGenerator.DEFAULT_SNOWFLAKE_ID_GENERATOR;
            } else {
                return new IdGenerator.SnowflakeIdGenerator(beginTime);
            }
        }
        return null;
    }

}
