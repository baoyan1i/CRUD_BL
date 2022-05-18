package io.simpleframework.crud.domain;

import io.simpleframework.crud.BaseModel;
import io.simpleframework.crud.BaseModelMapper;
import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.core.ConditionType;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;


@SuppressWarnings("all")
final class RepoSaveImpl {

    
    @SneakyThrows
    static Object save(DomainRepository repo, Object domainModel) {
        Object domainId = saveEntity(repo, domainModel);

        Set<String> updatedValueObjectNames = domainModel instanceof BaseDomain ?
                ((BaseDomain) domainModel).getUpdatedValueObjectNames() : null;
        for (Map.Entry<Field, DomainValueObjectRepository> entry : repo.getValueObjectRepos().entrySet()) {
            Field valueObjectField = entry.getKey();
            DomainValueObjectRepository valueObjectRepo = entry.getValue();
            if (updatedValueObjectNames == null || updatedValueObjectNames.contains(valueObjectRepo.getName())) {
                Object valueObject = valueObjectField.get(domainModel);
                saveValueObject(valueObjectRepo, valueObjectField, valueObject, domainId, domainId);
            }
        }
        return domainId;
    }

    @SneakyThrows
    private static Object saveEntity(DomainRepository repo, Object domainModel) {
        Object domainId;
        Object entity = repo.getEntityField().get(domainModel);
        boolean saveSuccess = false;
        if (entity instanceof BaseModel) {
            BaseModel m = (BaseModel) entity;
            saveSuccess = m.save();
            domainId = m.idValue();
        } else {
            ModelInfo modelInfo = repo.getEntityModelInfo();
            BaseModelMapper mapper = modelInfo.mapper();
            ModelField modelField = modelInfo.id();
            domainId = modelField.getValue(entity);
            if (domainId != null) {
                saveSuccess = mapper.updateById(entity);
            }
            if (!saveSuccess) {
                saveSuccess = mapper.insert(entity);
                domainId = modelField.getValue(entity);
            }
        }
        if (!saveSuccess) {
            throw new RuntimeException("Faild！");
        }
        return domainId;
    }

    @SneakyThrows
    private static void saveValueObject(DomainValueObjectRepository repo, Field parentField, Object parentFieldValue, Object domainId, Object entityId) {
        List valueObjects = extractValueObjects(repo, parentField, parentFieldValue);
        List valueObjectEntityList = extractValueObjectEntityList(repo, parentField, valueObjects, domainId, entityId);
        saveValueObjectEntityList(repo, valueObjectEntityList, domainId, entityId);
        for (Map.Entry<Field, DomainValueObjectRepository> entry : repo.getItems().entrySet()) {
            Field itemField = entry.getKey();
            DomainValueObjectRepository itemRepo = entry.getValue();
            for (Object valueObject : valueObjects) {
                if (repo.entityPresent()) {
                    Object valueObjectEntity = repo.getRepoModelEntityField().get(valueObject);
                    if (valueObjectEntity == null) {
                        continue;
                    }
                    if (valueObjectEntity instanceof BaseModel) {
                        entityId = ((BaseModel) valueObjectEntity).idValue();
                    } else {
                        entityId = repo.entityIdField().getValue(valueObjectEntity);
                    }
                }
                Object itemFieldValue = itemField.get(valueObject);
                saveValueObject(itemRepo, itemField, itemFieldValue, domainId, entityId);
            }
        }
    }

    @SneakyThrows
    private static List extractValueObjects(DomainValueObjectRepository repo, Field parentField, Object parentFieldValue) {
        if (parentFieldValue == null) {
            return Collections.emptyList();
        }
        List result = new ArrayList<>();
        if (parentFieldValue instanceof Collection) {
            result.addAll((Collection) parentFieldValue);
        } else {
            result.add(parentFieldValue);
        }
        return result;
    }

    @SneakyThrows
    private static List extractValueObjectEntityList(DomainValueObjectRepository repo, Field parentField, List valueObjects, Object domainId, Object entityId) {
        if (valueObjects.isEmpty() || repo.entityAbsent()) {
            return Collections.emptyList();
        }
        List result = new ArrayList();
        ModelInfo modelInfo = repo.getEntityModelInfo();
        ModelField domainField = modelInfo.getField(repo.getDomainFieldName());
        ModelField entityField = modelInfo.getField(repo.getEntityFieldName());
        Map<ModelField, Object> conditionFields = new HashMap<>();
        repo.getEntityConditions().getConditions().forEach((fieldName, fieldConditions) -> {
            fieldConditions.stream()
                    .filter(c -> c.getType() == ConditionType.equal)
                    .map(c -> c.getValue())
                    .findFirst()
                    .ifPresent(fieldValue -> {
                        conditionFields.put(modelInfo.getField(fieldName), fieldValue);
                    });
        });
        for (Object valueObject : valueObjects) {
            Object valueObjectEntity;
            if (parentField == repo.getRepoModelEntityField()) {
                valueObjectEntity = valueObject;
            } else {
                valueObjectEntity = repo.getRepoModelEntityField().get(valueObject);
            }
            if (valueObjectEntity == null) {
                continue;
            }

            if (domainField != null) {
                domainField.setValue(valueObjectEntity, domainId);
            }
            if (entityField != null) {
                entityField.setValue(valueObjectEntity, entityId);
            }
            for (Map.Entry<ModelField, Object> entry : conditionFields.entrySet()) {
                entry.getKey().setValue(valueObjectEntity, entry.getValue());
            }
            result.add(valueObjectEntity);
        }
        return result;
    }

    @SneakyThrows
    private static void saveValueObjectEntityList(DomainValueObjectRepository repo, List valueObjectEntityList, Object domainId, Object entityId) {
        if (repo.entityAbsent()) {
            return;
        }
        ModelInfo entityModelInfo = repo.getEntityModelInfo();
        boolean isBaseModel = entityModelInfo.isBaseModel();
        List newValues = new ArrayList();
        List updateValues = new ArrayList();
        List updateValueIds = new ArrayList();
        ModelField idField = repo.entityIdField();
        for (Object valueObjectEntity : valueObjectEntityList) {
            if (idField == null) {
                newValues.add(valueObjectEntity);
            }
            
            else {
                Object valueObjectId;
                if (isBaseModel) {
                    valueObjectId = ((BaseModel) valueObjectEntity).idValue();
                } else {
                    valueObjectId = idField.getValue(valueObjectEntity);
                }
                if (valueObjectId == null) {
                    newValues.add(valueObjectEntity);
                } else {
                    updateValues.add(valueObjectEntity);
                    updateValueIds.add(valueObjectId);
                }
            }
        }
        RepoDeleteImpl.deleteValueObject(repo, domainId, entityId, updateValueIds);
        if (isBaseModel) {
            for (Object value : updateValues) {
                ((BaseModel) value).updateById();
            }
            if (!newValues.isEmpty()) {
                DomainRepository.dao(repo.getEntityModelClass()).batchInsert(newValues);
            }
        } else {
            BaseModelMapper mapper = entityModelInfo.mapper();
            for (Object value : updateValues) {
                mapper.updateById(value);
            }
            if (!newValues.isEmpty()) {
                mapper.batchInsert(newValues);
            }
        }
    }

}
