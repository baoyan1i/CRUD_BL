package io.simpleframework.crud.domain;

import io.simpleframework.crud.BaseModel;
import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.core.ConditionType;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.util.SimpleCrudUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


final class RepoDeleteImpl {

    
    static void deleteById(DomainRepository repo, Serializable domainId) {
        ModelInfo<?> entityModelInfo = repo.getEntityModelInfo();
        if (entityModelInfo.isBaseModel()) {
            DomainRepository.dao(entityModelInfo.modelClass()).deleteById(domainId);
        } else {
            entityModelInfo.mapper().deleteById(domainId);
        }
        for (DomainValueObjectRepository valueObjectRepo : repo.getValueObjectRepos().values()) {
            deleteValueObject(valueObjectRepo, domainId, domainId);
        }
    }

    static void deleteValueObject(DomainValueObjectRepository repo, Object domainId, Object entityId) {
        deleteValueObject(repo, domainId, entityId, Collections.emptyList());
    }

    static void deleteValueObject(DomainValueObjectRepository repo, Object domainId, Object entityId, List<Object> retainIds) {
        deleteValueObjectItems(repo, domainId, entityId, retainIds);

        if (repo.entityAbsent()) {
            return;
        }
        Conditions deleteConditions = Conditions.of().addCondition(repo.getEntityConditions());
        if (SimpleCrudUtils.hasText(repo.getDomainFieldName())) {
            deleteConditions.addCondition(repo.getDomainFieldName(), domainId);
        }
        if (SimpleCrudUtils.hasText(repo.getEntityFieldName())) {
            deleteConditions.addCondition(repo.getEntityFieldName(), entityId);
        }
        if (!retainIds.isEmpty()) {
            deleteConditions.addCondition(repo.entityIdField().fieldName(), ConditionType.not_in, retainIds);
        }
        ModelInfo<?> entityModelInfo = repo.getEntityModelInfo();
        if (entityModelInfo.isBaseModel()) {
            DomainRepository.dao(entityModelInfo.modelClass()).deleteByConditions(deleteConditions);
        } else {
            entityModelInfo.mapper().deleteByConditions(deleteConditions);
        }
    }

    private static void deleteValueObjectItems(DomainValueObjectRepository repo, Object domainId, Object entityId, List<Object> retainIds) {
        if (repo.getItems().isEmpty()) {
            return;
        }
        if (repo.entityAbsent()) {
            for (DomainValueObjectRepository valueObjectRepo : repo.getItems().values()) {
                deleteValueObject(valueObjectRepo, domainId, entityId, retainIds);
            }
        } else {
            ModelField modelIdField = repo.entityIdField();
            for (Object model : repo.queryEntityModelsByDomain(domainId, entityId, true)) {
                Object modelId;
                if (model instanceof BaseModel) {
                    modelId = ((BaseModel<?>) model).idValue();
                } else {
                    modelId = modelIdField.getValue(model);
                }
                if (modelId == null || retainIds.contains(modelId)) {
                    continue;
                }
                for (DomainValueObjectRepository valueObjectRepo : repo.getItems().values()) {
                    deleteValueObject(valueObjectRepo, domainId, modelId);
                }
            }
        }
    }

}
