package io.simpleframework.crud.domain;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.annotation.Condition;
import io.simpleframework.crud.annotation.DomainEntity;
import io.simpleframework.crud.annotation.DomainValueObject;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.QueryConfig;
import io.simpleframework.crud.exception.DomainClassIllegalException;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Getter
@Setter(AccessLevel.PRIVATE)
@SuppressWarnings("all")
class DomainValueObjectRepository {

    
    private final String name;
    
    private final Class repoModelClass;
    
    private Field repoModelEntityField;
    
    private final String domainFieldName;
    
    private final String entityFieldName;
    
    private Class entityModelClass;
    
    private ModelInfo entityModelInfo;
    
    private final Conditions entityConditions = Conditions.of();
    
    private Map<Field, DomainValueObjectRepository> items = new LinkedHashMap<>();

    @SneakyThrows
    public List queryEntityModelsByDomain(Object domainId, Object entityId, boolean onlySelectId) {
        QueryConfig queryConfig = QueryConfig.of().addCondition(this.entityConditions);
        if (onlySelectId) {
            queryConfig.addSelect(this.entityIdField().fieldName());
        }
        if (SimpleCrudUtils.hasText(this.domainFieldName)) {
            queryConfig.addCondition(this.domainFieldName, domainId);
        }
        if (SimpleCrudUtils.hasText(this.entityFieldName)) {
            queryConfig.addCondition(this.entityFieldName, entityId);
        }
        if (this.entityModelInfo.isBaseModel()) {
            return DomainRepository.dao(this.entityModelClass).listByCondition(queryConfig);
        } else {
            return this.entityModelInfo.mapper().listByConfig(queryConfig);
        }
    }

    ModelField entityIdField() {
        return this.entityModelInfo.id();
    }

    boolean entityAbsent() {
        return this.entityModelClass == null;
    }

    boolean entityPresent() {
        return this.entityModelClass != null;
    }

    static DomainValueObjectRepository of(Field parentField, DomainValueObjectRepository parentRepo) {
        DomainValueObjectRepository repo = new DomainValueObjectRepository(parentField);
        List<Field> fields = SimpleCrudUtils.getFields(repo.getRepoModelClass(), f -> {
            return f.isAnnotationPresent(DomainEntity.class) || f.isAnnotationPresent(DomainValueObject.class);
        });
        if (fields.isEmpty()) {
            repo.setEntity(parentField, repo.getRepoModelClass());
        } else {
            repo.setItems(fields, parentRepo);
        }
        repo.setEntityConditions(parentField.getAnnotationsByType(Condition.class));
        return repo;
    }

    private void setEntity(Field field, Class<?> entityClass) {
        if (this.getEntityModelClass() != null) {
            throw new IllegalArgumentException("Found more than one field declared by @DomainEntity from " + entityClass);
        }
        ModelInfo<?> modelInfo = DomainRepository.buildInfo(field, entityClass);
        this.setRepoModelEntityField(field);
        this.setEntityModelClass(entityClass);
        this.setEntityModelInfo(modelInfo);
    }

    private void setItems(List<Field> fields, DomainValueObjectRepository parentRepo) {
        boolean sameAsParent = parentRepo != null && this.getRepoModelClass() == parentRepo.getRepoModelClass();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DomainEntity.class)) {
                this.setEntity(field, field.getType());
            } else if (field.isAnnotationPresent(DomainValueObject.class)) {
                if (sameAsParent) {
                    continue;
                }
                DomainValueObjectRepository item = this.buildItem(field);
                this.items.put(field, item);
            }
        }
        if (sameAsParent) {
            this.items = parentRepo.getItems();
        }
        if (!this.items.isEmpty()) {
            if (this.entityModelClass != null && this.entityIdField() == null) {
                throw new IllegalArgumentException("Can not found id field from " + this.entityModelClass);
            }
        }
    }

    private DomainValueObjectRepository buildItem(Field field) {
        DomainValueObjectRepository repo = DomainValueObjectRepository.of(field, this);
        String entityField = repo.getEntityFieldName();
        if (repo.getEntityModelInfo().getField(entityField) == null) {
            String msg = String.format("Can not found entityField [%s] from %s.%s", entityField, this.entityModelClass, field.getName());
            throw new IllegalArgumentException(msg);
        }
        return repo;
    }

    private void setEntityConditions(Condition[] conditions) {
        if (this.entityModelInfo == null) {
            if (conditions.length > 0) {
                throw new IllegalArgumentException(this.repoModelClass + " is not support @Condition , because it can not found @DomainEntity ");
            }
            return;
        }
        for (Condition condition : conditions) {
            String conditionFieldName = condition.field();
            String conditionValueStr = condition.defaultValueIfNull();
            if (SimpleCrudUtils.isBlank(conditionValueStr)) {
                continue;
            }
            ModelField conditionField = this.entityModelInfo.getField(conditionFieldName);
            if (conditionField == null) {
                throw new IllegalArgumentException(
                        String.format("Can not found conditionField [%s] from %s", conditionFieldName, this.entityModelClass)
                );
            }
            Object conditionValue = SimpleCrudUtils.cast(conditionValueStr, conditionField.fieldType());
            this.entityConditions.addCondition(conditionFieldName, condition.type(), conditionValue);
        }
    }

    private DomainValueObjectRepository(Field field) {
        Class<?> clazz = field.getType();
        if (Collection.class.isAssignableFrom(clazz)) {
            clazz = SimpleCrudUtils.getGenericClass(field, clazz);
        }
        try {
            clazz.getDeclaredConstructor();
        } catch (Exception e) {
            throw new DomainClassIllegalException(clazz, e);
        }
        this.repoModelClass = clazz;
        DomainValueObject config = field.getAnnotation(DomainValueObject.class);
        String name = config.name();
        if (SimpleCrudUtils.isBlank(name)) {
            name = field.getName();
        }
        this.name = name;
        this.domainFieldName = config.domainField();
        this.entityFieldName = config.entityField();
    }

}
