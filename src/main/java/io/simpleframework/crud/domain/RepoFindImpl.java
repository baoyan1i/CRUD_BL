package io.simpleframework.crud.domain;

import io.simpleframework.crud.BaseModel;
import io.simpleframework.crud.ModelInfo;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("all")
final class RepoFindImpl {
    private static final Map<Class<?>, WeakReference<Constructor>> CONSTRUCTORS = new ConcurrentHashMap<>();

    @SneakyThrows
    public static Object findById(DomainRepository repo, Object domainModel, Serializable domainId) {
        if (domainId == null) {
            return null;
        }
        Object entity;
        ModelInfo entityModelInfo = repo.getEntityModelInfo();
        if (entityModelInfo.isBaseModel()) {
            entity = DomainRepository.dao(entityModelInfo.modelClass()).findById(domainId);
        } else {
            entity = entityModelInfo.mapper().findById(domainId);
        }
        if (entity == null) {
            return null;
        }
        repo.getEntityField().set(domainModel, entity);

        for (Map.Entry<Field, DomainValueObjectRepository> entry : repo.getValueObjectRepos().entrySet()) {
            Field field = entry.getKey();
            Object valueObject = getValueObject(entry.getValue(), field, domainId, domainId);
            field.set(domainModel, valueObject);
        }
        return domainModel;
    }

    private static Object getValueObject(DomainValueObjectRepository repo, Field parentField, Object domainId, Object entityId) {
        Class<?> parentFieldClass = parentField.getType();
        if (parentFieldClass == List.class) {
            parentFieldClass = ArrayList.class;
        } else if (parentFieldClass == Set.class) {
            parentFieldClass = LinkedHashSet.class;
        }
        // 上级类字段 = 值对象模型字段：普通的值对象形式
        if (parentField == repo.getRepoModelEntityField()) {
            List models = repo.queryEntityModelsByDomain(domainId, entityId, false);
            return Collection.class.isAssignableFrom(parentFieldClass) ? models :
                    models.isEmpty() ? null : models.get(0);
        }
        
        return getValueObject(repo, parentFieldClass, domainId, entityId);
    }

    @SneakyThrows
    private static Object getValueObject(DomainValueObjectRepository repo, Class<?> returnClass, Object domainId, Object entityId) {
        boolean resultIsCollection = Collection.class.isAssignableFrom(returnClass);
        Object result = resultIsCollection ? returnClass.newInstance() : null;
        if (repo.entityAbsent()) {
            Object instance = buildValueObjectInstance(repo, null, domainId, entityId);
            if (resultIsCollection) {
                ((Collection) result).add(instance);
            } else {
                result = instance;
            }
            return result;
        }

        List models = repo.queryEntityModelsByDomain(domainId, entityId, false);
        if (models.isEmpty()) {
            return result;
        }
        if (resultIsCollection) {
            for (Object model : models) {
                Object instance = buildValueObjectInstance(repo, model, domainId, entityId);
                ((Collection) result).add(instance);
            }
        } else {
            result = models.isEmpty() ? null :
                    buildValueObjectInstance(repo, models.get(0), domainId, entityId);
        }
        return result;
    }

    @SneakyThrows
    private static Object buildValueObjectInstance(DomainValueObjectRepository repo, Object valueObjectEntity, Object domainId, Object entityId) {
        Object result = newInstance(repo.getRepoModelClass());
        if (repo.entityPresent()) {
            repo.getRepoModelEntityField().set(result, valueObjectEntity);
            entityId = repo.entityIdField().getValue(valueObjectEntity);
        }
        for (Map.Entry<Field, DomainValueObjectRepository> entry : repo.getItems().entrySet()) {
            Field field = entry.getKey();
            Object valueObjectItem = getValueObject(entry.getValue(), field, domainId, entityId);
            field.set(result, valueObjectItem);
        }
        return result;
    }

    @SneakyThrows
    private static <T> Object newInstance(Class<T> clazz) {
        Constructor constructor = Optional.ofNullable(CONSTRUCTORS.get(clazz))
                .map(WeakReference::get)
                .orElseGet(() -> {
                    Constructor<T> c = null;
                    try {
                        c = clazz.getDeclaredConstructor();
                    } catch (Exception ignore) {
                    }
                    c.setAccessible(true);
                    CONSTRUCTORS.put(clazz, new WeakReference<>(c));
                    return c;
                });
        return constructor.newInstance();
    }

}
