package io.simpleframework.crud.mapper;

import io.simpleframework.crud.BaseModelMapper;
import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.QueryConfig;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collection;


public abstract class AbstractModelMapper<T> implements BaseModelMapper<T> {
    @Getter(AccessLevel.PROTECTED)
    private final ModelInfo<T> modelInfo;

    protected AbstractModelMapper(ModelInfo<T> modelInfo) {
        this.modelInfo = modelInfo;
    }

    @SuppressWarnings("unchecked")
    protected void setIdValueIfAbsent(Object model) {
        ModelField id = this.modelInfo.id();
        if (id == null) {
            return;
        }
        if (model instanceof Collection) {
            for (T m : (Collection<T>) model) {
                id.setValue(m, null);
            }
        } else {
            id.setValue(model, null);
        }
    }

    protected QueryConfig combineQueryConfigs(QueryConfig... configs) {
        int configLength = configs == null ? 0 : configs.length;
        if (configLength == 0) {
            return QueryConfig.of();
        }
        return configs[0].combine(configs);
    }

    protected Conditions combineConditions(Conditions... conditions) {
        int configLength = conditions == null ? 0 : conditions.length;
        if (configLength == 0) {
            return Conditions.of();
        }
        return conditions[0].combine(conditions);
    }

}
