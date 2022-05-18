package io.simpleframework.crud.info.clazz;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.annotation.Id;
import io.simpleframework.crud.annotation.IdStrategy;
import io.simpleframework.crud.annotation.Table;
import io.simpleframework.crud.core.ModelConfiguration;
import io.simpleframework.crud.core.NameType;
import io.simpleframework.crud.info.AbstractModelInfo;
import io.simpleframework.crud.info.ModelId;
import io.simpleframework.crud.util.SimpleCrudUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ClassModelInfo<T> extends AbstractModelInfo<T> {
    
    private static final String DEFAULT_ID_FIELD = "id";

    public ClassModelInfo(Class<T> modelClass, ModelConfiguration modelConfig) {
        super(modelClass, modelConfig, obtainModelName(modelClass, modelConfig.tableNameType()));
        List<ModelField> modelFields = obtainFields(modelClass, modelConfig.columnNameType());
        super.setId(obtainModelId(modelFields));
        super.addField(modelFields);
    }

    
    private static String obtainModelName(Class<?> clazz, NameType nameType) {
        BiFunction<String, String, String> nameTrans = (modelName, definedName) -> {
            if (SimpleCrudUtils.hasText(definedName)) {
                modelName = definedName;
            }
            return modelName;
        };
        BiFunction<String, String, String> schemaTrans = (modelName, definedSchema) -> {
            if (SimpleCrudUtils.hasText(definedSchema)) {
                modelName = definedSchema + "." + modelName;
            }
            return modelName;
        };

        String modelName = nameType.trans(clazz.getSimpleName());
        Table crudTable = clazz.getAnnotation(Table.class);
        if (crudTable != null) {
            modelName = nameTrans.apply(modelName, crudTable.name());
            modelName = schemaTrans.apply(modelName, crudTable.schema());
        } else if (SimpleCrudUtils.jpaPresent) {
            javax.persistence.Table jpaTable = clazz.getAnnotation(javax.persistence.Table.class);
            if (jpaTable != null) {
                modelName = nameTrans.apply(modelName, jpaTable.name());
                modelName = schemaTrans.apply(modelName, jpaTable.schema());
            }
        } else if (SimpleCrudUtils.mybatisPlusPresent) {
            com.baomidou.mybatisplus.annotation.TableName mpTable =
                    clazz.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
            if (mpTable != null) {
                modelName = nameTrans.apply(modelName, mpTable.value());
                modelName = schemaTrans.apply(modelName, mpTable.schema());
            }
        }
        return modelName;
    }

    
    private static List<ModelField> obtainFields(Class<?> clazz, NameType nameType) {
        Predicate<Field> fieldFilter = field -> {
            int modifiers = field.getModifiers();
            // 过滤掉静态字段
            boolean isStatic = Modifier.isStatic(modifiers);
            // 过滤掉 transient关键字修饰的字段
            boolean isTransient = Modifier.isTransient(modifiers);
            if (!isTransient) {
                // 过滤掉 @Transient 注解的字段
                isTransient = SimpleCrudUtils.jpaPresent && field.isAnnotationPresent(javax.persistence.Transient.class);
            }
            return !isStatic && !isTransient;
        };
        return SimpleCrudUtils.getFields(clazz, fieldFilter)
                .stream()
                .map(f -> new ClassModelField(f, nameType))
                .collect(Collectors.toList());
    }

    
    private static ModelId obtainModelId(List<ModelField> modelFields) {
        ClassModelField idField = null;
        for (ModelField modelField : modelFields) {
            ClassModelField field = (ClassModelField) modelField;
            if (field.getField().isAnnotationPresent(Id.class)) {
                idField = field;
                break;
            }
        }
        if (idField == null) {
            for (ModelField modelField : modelFields) {
                ClassModelField field = (ClassModelField) modelField;
                if (SimpleCrudUtils.jpaPresent && field.getField().isAnnotationPresent(javax.persistence.Id.class)) {
                    idField = field;
                    break;
                }
                if (DEFAULT_ID_FIELD.equals(modelField.fieldName())) {
                    idField = field;
                }
            }
        }
        if (idField == null) {
            return null;
        }
        IdStrategy strategy = idField.getField().getAnnotation(IdStrategy.class);
        return strategy == null ? new ModelId(idField) : new ModelId(idField, strategy.type(), strategy.beginTime());
    }

}
