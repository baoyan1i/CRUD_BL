package io.simpleframework.crud.info.clazz;

import io.simpleframework.crud.annotation.Column;
import io.simpleframework.crud.core.NameType;
import io.simpleframework.crud.info.AbstractModelField;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.SneakyThrows;

import java.lang.reflect.Field;


public class ClassModelField extends AbstractModelField {
    private final Field field;

    ClassModelField(Field field, NameType nameType) {
        this.field = field;
        field.setAccessible(true);
        String fieldName = field.getName();

        boolean insertable = true;
        boolean updatable = true;
        String columnName = null;
        Column crudColumn = this.field.getAnnotation(Column.class);
        if (crudColumn != null) {
            columnName = crudColumn.name();
            insertable = crudColumn.insertable();
            updatable = crudColumn.updatable();
        } else if (SimpleCrudUtils.jpaPresent) {
            javax.persistence.Column jpaColumn = this.field.getAnnotation(javax.persistence.Column.class);
            if (jpaColumn != null) {
                columnName = jpaColumn.name();
                insertable = jpaColumn.insertable();
                updatable = jpaColumn.updatable();
            }
        } else if (SimpleCrudUtils.mybatisPlusPresent) {
            com.baomidou.mybatisplus.annotation.TableField mpColumn =
                    this.field.getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class);
            if (mpColumn != null) {
                columnName = mpColumn.value();
            }
        }
        if (SimpleCrudUtils.isBlank(columnName)) {
            columnName = nameType.trans(fieldName);
        }
        super.setColumn(columnName, fieldName, field.getType());
        super.setInsertable(insertable);
        super.setUpdatable(updatable);
    }

    @SneakyThrows
    @Override
    public <T> Object getValue(T model) {
        return this.field.get(model);
    }

    @SneakyThrows
    @Override
    public <T> void setValue(T model, Object value) {
        this.field.set(model, value);
    }

    Field getField() {
        return this.field;
    }

}
