package io.simpleframework.crud.domain;

import io.simpleframework.crud.BaseModel;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.Models;
import io.simpleframework.crud.annotation.DomainEntity;
import io.simpleframework.crud.annotation.DomainValueObject;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.Getter;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Getter
@SuppressWarnings("all")
public class DomainRepository {
    private static final Map<Class<?>, WeakReference<BaseModel>> DAOS = new ConcurrentHashMap<>();

    
    private Field entityField;
    
    private ModelInfo entityModelInfo;
    
    private final Map<Field, DomainValueObjectRepository> valueObjectRepos = new LinkedHashMap<>();

    static BaseModel dao(Class<?> clazz) {
        return Optional.ofNullable(DAOS.get(clazz))
                .map(WeakReference::get)
                .orElseGet(() -> {
                    BaseModel dao = null;
                    if (BaseModel.class.isAssignableFrom(clazz)) {
                        try {
                            dao = (BaseModel) clazz.newInstance();
                        } catch (Exception ignore) {

                        }
                    }
                    if (dao != null) {
                        DAOS.put(clazz, new WeakReference<>(dao));
                    }
                    return dao;
                });
    }

    public Object findById(Object domainModel, Serializable domainId) {
        return RepoFindImpl.findById(this, domainModel, domainId);
    }

    public void deleteById(Serializable domainId) {
        RepoDeleteImpl.deleteById(this, domainId);
    }

    public Object save(Object domainModel) {
        return RepoSaveImpl.save(this, domainModel);
    }

    public static DomainRepository of(Class<?> domainClass) {
        DomainRepository result = new DomainRepository();
        List<Field> fields = SimpleCrudUtils.getFields(domainClass, f -> {
            return f.isAnnotationPresent(DomainEntity.class) || f.isAnnotationPresent(DomainValueObject.class);
        });
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(DomainEntity.class)) {
                result.setEntity(domainClass, field);
            } else if (field.isAnnotationPresent(DomainValueObject.class)) {
                result.addValueObject(domainClass, field);
            }
        }
        if (result.getEntityField() == null) {
            throw new IllegalArgumentException(domainClass + " can not found a field to be AGGREGATE_ROOT");
        }
        return result;
    }

    private void setEntity(Class<?> domainClass, Field field) {
        if (this.entityField != null) {
            throw new IllegalArgumentException("Found more than one field declared by @DomainEntity from " + domainClass);
        }
        this.entityField = field;
        this.entityModelInfo = buildInfo(field, field.getType());
        if (this.entityModelInfo.id() == null) {
            throw new IllegalArgumentException("Can not found id field from " + field.getType());
        }
    }

    private void addValueObject(Class<?> domainClass, Field field) {
        DomainValueObjectRepository repo = DomainValueObjectRepository.of(field, null);
        if (repo.entityPresent()) {
            ModelInfo info = repo.getEntityModelInfo();
            String domainField = repo.getDomainFieldName();
            String entityField = repo.getEntityFieldName();
            if (info.getField(domainField) == null && info.getField(entityField) == null) {
                String msg = String.format("Can not found domainField or entityField from %s.%s", domainClass, field.getName());
                throw new IllegalArgumentException(msg);
            }
        }
        this.valueObjectRepos.put(field, repo);
    }

    static ModelInfo<?> buildInfo(Field field, Class clazz) {
        ModelInfo<?> result = Models.info(clazz);
        if (result == null) {
            throw new IllegalArgumentException("Domain is not support " + clazz);
        }
        return result;
    }

}
