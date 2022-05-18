package io.simpleframework.crud.info.dynamic;

import io.simpleframework.crud.info.AbstractModelField;

import java.util.Map;


@SuppressWarnings("all")
public class DynamicModelField extends AbstractModelField {

    DynamicModelField(String fieldName, String column, Class<?> fieldType) {
        super();
        super.setColumn(column, fieldName, fieldType);
        super.setInsertable(true);
        super.setUpdatable(true);
    }

    @Override
    public <T> Object getValue(T model) {
        this.validModel(model);
        return ((Map) model).get(super.fieldName());
    }

    @Override
    public <T> void setValue(T model, Object value) {
        this.validModel(model);
        this.validValue(value);
        ((Map) model).put(super.fieldName(), value);
    }

    private <T> void validModel(T model) {
        if (Map.class.isAssignableFrom(model.getClass())) {
            return;
        }
        throw new IllegalArgumentException("Dynamic model is not a Map instance");
    }

    private void validValue(Object value) {
        if (value == null || super.fieldType().isAssignableFrom(value.getClass())) {
            return;
        }
        String msg = String.format("Dynamic model field [%s] must be [%s]", super.fieldName(), super.fieldType());
        throw new IllegalArgumentException(msg);
    }

}
