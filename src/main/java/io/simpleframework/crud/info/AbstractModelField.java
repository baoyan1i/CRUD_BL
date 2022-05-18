package io.simpleframework.crud.info;

import io.simpleframework.crud.ModelField;


public abstract class AbstractModelField implements ModelField {
    
    private String column;
    
    private String fieldName;
    
    private Class<?> fieldType;
    
    private boolean insertable;
    
    private boolean updatable;

    @Override
    public String column() {
        return this.column;
    }

    @Override
    public String fieldName() {
        return this.fieldName;
    }

    @Override
    public Class<?> fieldType() {
        return this.fieldType;
    }

    @Override
    public boolean insertable() {
        return this.insertable;
    }

    @Override
    public boolean updatable() {
        return this.updatable;
    }

    protected void setInsertable(boolean insertable) {
        this.insertable = insertable;
    }

    protected void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    protected void setColumn(String column, String fieldName, Class<?> fieldType) {
        this.column = column;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

}
