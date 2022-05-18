package io.simpleframework.crud.core;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.annotation.Condition;
import io.simpleframework.crud.annotation.ConditionConfigs;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Data
public class Conditions implements Serializable {

    
    private final Map<String, List<ConditionInfo>> conditions = new LinkedHashMap<>();

    public static Conditions of() {
        return new Conditions();
    }

    public static Conditions fromAnnotation(Object annotation) {
        return new Conditions().addConditionByAnnotation(annotation);
    }

    public Conditions combine(Conditions... conditions) {
        for (Conditions condition : conditions) {
            if (condition == this) {
                continue;
            }
            this.addCondition(condition);
        }
        return this;
    }

    
    public Map<String, Object> getConditionData() {
        Map<String, Object> result = new HashMap<>(8);
        for (Map.Entry<String, List<ConditionInfo>> entry : this.conditions.entrySet()) {
            ConditionInfo.append(result, entry.getKey(), entry.getValue());
        }
        return result;
    }

    
    public List<ModelField> getFields(List<ModelField> fields) {
        List<ModelField> result = new ArrayList<>();
        for (ModelField field : fields) {
            if (this.conditions.containsKey(field.fieldName())) {
                result.add(field);
            }
        }
        return result;
    }

    
    public List<ConditionInfo> getConditionInfos(String fieldName, boolean needNullValue) {
        List<ConditionInfo> result = this.conditions.getOrDefault(fieldName, new ArrayList<>());
        if (needNullValue) {
            if (result.isEmpty()) {
                result.add(ConditionInfo.of(ConditionType.equal, null));
            }
            return result;
        } else {
            return result.stream().filter(c -> {
                boolean isNullType = c.getType() == ConditionType.is_null || c.getType() == ConditionType.not_null;
                return isNullType || c.getValue() != null;
            }).collect(Collectors.toList());
        }
    }

    public Conditions addCondition(String fieldName, Object value) {
        if (value instanceof ConditionType) {
            return this.addCondition(fieldName, (ConditionType) value, (Object) null);
        }
        if (value instanceof Collection) {
            return this.addCondition(fieldName, ConditionType.in, value);
        } else {
            return this.addCondition(fieldName, ConditionType.equal, value);
        }
    }

    public <T, R> Conditions addCondition(SerializedFunction<T, R> fieldNameFunc, Object value) {
        if (value instanceof ConditionType) {
            return this.addCondition(fieldNameFunc, (ConditionType) value, (Object) null);
        }
        if (value instanceof Collection) {
            return this.addCondition(fieldNameFunc, ConditionType.in, value);
        } else {
            return this.addCondition(fieldNameFunc, ConditionType.equal, value);
        }
    }

    public <T, R> Conditions addCondition(SerializedFunction<T, R> fieldNameFunc, ConditionType conditionType, Object... values) {
        String fieldName = SimpleCrudUtils.getLambdaFieldName(fieldNameFunc);
        return this.addCondition(fieldName, conditionType, values);
    }

    public Conditions addConditionByAnnotation(Object annotation) {
        annotationFieldConditions(annotation).forEach((fieldName, fieldConditions) -> {
            fieldConditions.forEach(fieldCondition -> {
                this.addCondition(fieldName, fieldCondition.getType(), fieldCondition.getValue());
            });
        });
        return this;
    }

    public Conditions addCondition(Conditions conditions) {
        if (conditions == this) {
            return this;
        }
        conditions.getConditions().forEach((fieldName, fieldConditions) -> {
            fieldConditions.forEach(fieldCondition -> {
                this.addCondition(fieldName, fieldCondition.getType(), fieldCondition.getValue());
            });
        });
        return this;
    }

    public synchronized Conditions addCondition(String fieldName, ConditionType conditionType, Object... values) {
        if (conditionType == null) {
            conditionType = ConditionType.equal;
        }
        List<ConditionInfo> infos = this.conditions.computeIfAbsent(fieldName, k -> new ArrayList<>());
        Object value = transToValue(conditionType, values);
        ConditionInfo info = ConditionInfo.of(infos.size(), conditionType, value);
        infos.add(info);
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Object transToValue(ConditionType type, Object... values) {
        if (values == null) {
            return null;
        }
        values = Arrays.stream(values).filter(Objects::nonNull).toArray();
        int valuesLength = values.length;
        if (valuesLength == 0) {
            return null;
        }
        if (type == ConditionType.in || type == ConditionType.not_in) {
            List<Object> result = new ArrayList<>();
            for (Object temp : values) {
                if (temp instanceof Collection) {
                    result.addAll((Collection<Object>) temp);
                } else {
                    result.add(temp);
                }
            }
            return result.isEmpty() ? null : result;
        }
        return valuesLength == 1 ? values[0] : Arrays.asList(values);
    }

    private static final Map<Class<?>, List<Field>> CONDITION_CACHES = new ConcurrentHashMap<>();

    @SneakyThrows
    private static Map<String, List<ConditionInfo>> annotationFieldConditions(Object annotation) {
        if (annotation == null) {
            return Collections.emptyMap();
        }
        Class<?> annotationClass = annotation.getClass();
        List<Field> fields = CONDITION_CACHES.computeIfAbsent(annotationClass,
                c -> SimpleCrudUtils.getFields(annotationClass, f ->
                        f.isAnnotationPresent(Condition.class) || f.isAnnotationPresent(ConditionConfigs.class)));
        if (fields.isEmpty()) {
            throw new IllegalArgumentException(annotationClass.getName() + " can not found any field declared by @Condition");
        }
        Map<String, List<ConditionInfo>> result = new LinkedHashMap<>();
        for (Field field : fields) {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            for (Condition conditionData : field.getAnnotationsByType(Condition.class)) {
                String fieldName = conditionData.field();
                if (SimpleCrudUtils.isBlank(fieldName)) {
                    fieldName = field.getName();
                }
                Object conditionValue = field.get(annotation);
                if (conditionValue == null) {
                    String defaultValue = conditionData.defaultValueIfNull();
                    if (SimpleCrudUtils.hasText(defaultValue)) {
                        conditionValue = SimpleCrudUtils.cast(defaultValue, field.getType());
                    }
                }
                ConditionInfo condition = ConditionInfo.of(conditionData.type(), conditionValue);
                result.computeIfAbsent(fieldName, f -> new ArrayList<>())
                        .add(condition);
            }
            if (!accessible) {
                field.setAccessible(false);
            }
        }
        return result;
    }

    @Data
    public static class ConditionInfo implements Serializable {
        private int index;
        private ConditionType type;
        private Object value;

        public static ConditionInfo of(ConditionType type, Object value) {
            return of(0, type, value);
        }

        public static ConditionInfo of(int index, ConditionType type, Object value) {
            ConditionInfo result = new ConditionInfo();
            result.setIndex(index);
            result.setType(type);
            result.setValue(value);
            return result;
        }

        static void append(Map<String, Object> result, String fieldName, List<ConditionInfo> conditions) {
            for (ConditionInfo condition : conditions) {
                result.put(condition.getKey(fieldName), condition.getValue());
            }
        }

        public String getKey(String fieldName) {
            return fieldName + this.getIndex();
        }
    }

}
