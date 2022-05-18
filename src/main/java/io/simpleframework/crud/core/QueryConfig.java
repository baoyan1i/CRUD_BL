package io.simpleframework.crud.core;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.*;


@Data
public class QueryConfig implements Serializable {
    public static final String SELECT_ALL_FIELD = "*";
    
    private final Set<String> selectFieldNames = new HashSet<>();
    
    private Conditions conditions = Conditions.of();
    
    private final QuerySorter sorter = QuerySorter.of();

    public static QueryConfig of() {
        return new QueryConfig();
    }

    public static QueryConfig fromAnnotation(Object annotation) {
        return new QueryConfig().addConditionByAnnotation(annotation);
    }

    public QueryConfig combine(QueryConfig... configs) {
        for (QueryConfig config : configs) {
            if (config == this) {
                continue;
            }
            this.addSelect(config.getSelectFieldNames());
            this.addCondition(config.getConditions());
            this.addSorter(config.getSorter());
        }
        return this;
    }

    
    public List<ModelField> getSelectFields(List<ModelField> fields) {
        if (this.selectFieldNames.isEmpty() || this.selectFieldNames.contains(SELECT_ALL_FIELD)) {
            return fields;
        }
        List<ModelField> selectFields = new ArrayList<>();
        for (ModelField field : fields) {
            if (this.selectFieldNames.contains(field.fieldName())) {
                selectFields.add(field);
            }
        }
        return selectFields;
    }

    
    public QueryConfig addSelect(String... fieldNames) {
        return this.addSelect(Arrays.asList(fieldNames));
    }

    public <T, R> QueryConfig addSelect(SerializedFunction<T, R> fieldNameFunc) {
        String fieldName = SimpleCrudUtils.getLambdaFieldName(fieldNameFunc);
        return this.addSelect(fieldName);
    }

    public QueryConfig addSelect(Collection<String> fieldNames) {
        this.selectFieldNames.addAll(fieldNames);
        return this;
    }

    public QueryConfig addCondition(String fieldName, Object value) {
        this.conditions.addCondition(fieldName, value);
        return this;
    }

    public <T, R> QueryConfig addCondition(SerializedFunction<T, R> fieldNameFunc, Object value) {
        this.conditions.addCondition(fieldNameFunc, value);
        return this;
    }

    public synchronized QueryConfig addCondition(String fieldName, ConditionType conditionType, Object... values) {
        this.conditions.addCondition(fieldName, conditionType, values);
        return this;
    }

    public <T, R> QueryConfig addCondition(SerializedFunction<T, R> fieldNameFunc, ConditionType conditionType, Object... values) {
        this.conditions.addCondition(fieldNameFunc, conditionType, values);
        return this;
    }

    public QueryConfig addCondition(Conditions conditions) {
        this.conditions.addCondition(conditions);
        return this;
    }

    public QueryConfig addConditionByAnnotation(Object annotation) {
        this.conditions.addConditionByAnnotation(annotation);
        return this;
    }

    public QueryConfig addSorter(QuerySorter sorter) {
        this.sorter.getItems().putAll(sorter.getItems());
        return this;
    }

    private void merge(QueryConfig config) {

    }

}
