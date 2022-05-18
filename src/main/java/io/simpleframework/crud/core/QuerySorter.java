package io.simpleframework.crud.core;

import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class QuerySorter implements Serializable {
    @Getter
    private final Map<String, Boolean> items = new LinkedHashMap<>();

    public static QuerySorter of() {
        return new QuerySorter();
    }

    public static QuerySorter asc(String... fieldNames) {
        return of().addAsc(fieldNames);
    }

    public static <T, R> QuerySorter asc(SerializedFunction<T, R> fieldNameFunc) {
        return of().addAsc(fieldNameFunc);
    }

    public static QuerySorter desc(String... fieldNames) {
        return of().addDesc(fieldNames);
    }

    public static <T, R> QuerySorter desc(SerializedFunction<T, R> fieldNameFunc) {
        return of().addDesc(fieldNameFunc);
    }

    public QuerySorter addAsc(String... fieldNames) {
        for (String fieldName : fieldNames) {
            items.put(fieldName, true);
        }
        return this;
    }

    public <T, R> QuerySorter addAsc(SerializedFunction<T, R> fieldNameFunc) {
        String fieldName = SimpleCrudUtils.getLambdaFieldName(fieldNameFunc);
        return this.addAsc(fieldName);
    }

    public QuerySorter addDesc(String... fieldNames) {
        for (String fieldName : fieldNames) {
            items.put(fieldName, false);
        }
        return this;
    }

    public <T, R> QuerySorter addDesc(SerializedFunction<T, R> fieldNameFunc) {
        String fieldName = SimpleCrudUtils.getLambdaFieldName(fieldNameFunc);
        return this.addDesc(fieldName);
    }

}
