package io.simpleframework.crud;

import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.Page;
import io.simpleframework.crud.core.QueryConfig;
import io.simpleframework.crud.core.QuerySorter;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


public interface BaseModelMapper<T> {

    
    boolean insert(T model);

    
    boolean batchInsert(List<? extends T> models);

    
    boolean deleteById(Serializable id);

    
    boolean deleteByIds(Collection<? extends Serializable> ids);

    
    int deleteByConditions(Conditions conditions);

    
    default int deleteByAnnotation(Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.deleteByConditions(conditions);
    }

    
    boolean updateById(T model);

    
    boolean updateByIdWithNull(T model);

    
    int updateByConditions(T model, Conditions conditions);

    
    default int updateByAnnotation(T model, Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.updateByConditions(model, conditions);
    }

    
    <R extends T> R findById(Serializable id);

    
    <R extends T> List<R> listByIds(Collection<? extends Serializable> ids);

    
    <R extends T> List<R> listByCondition(R model, QueryConfig... configs);

    
    default <R extends T> List<R> listByConfig(QueryConfig config) {
        return this.listByCondition(null, config);
    }

    
    default <R extends T> List<R> listByAnnotation(R model, Object annotation) {
        QueryConfig config = QueryConfig.fromAnnotation(annotation);
        return this.listByCondition(model, config);
    }

    
    default <R extends T> List<R> listByAnnotation(Object annotation) {
        return this.listByAnnotation(null, annotation);
    }

    
    default <R extends T> List<R> listBySorter(R model, QuerySorter sorter) {
        QueryConfig config = QueryConfig.of().addSorter(sorter);
        return this.listByCondition(model, config);
    }

    
    default <R extends T> List<R> listBySorter(QuerySorter sorter) {
        return this.listBySorter(null, sorter);
    }

    
    <R extends T> Page<R> pageByCondition(R model, int pageNum, int pageSize, QueryConfig... configs);

    
    default <R extends T> Page<R> pageByConfig(int pageNum, int pageSize, QueryConfig... configs) {
        return this.pageByCondition(null, pageNum, pageSize, configs);
    }

    
    default <R extends T> Page<R> pageByAnnotation(R model, int pageNum, int pageSize, Object annotation) {
        QueryConfig config = QueryConfig.fromAnnotation(annotation);
        return this.pageByCondition(model, pageNum, pageSize, config);
    }

    
    default <R extends T> Page<R> pageByAnnotation(int pageNum, int pageSize, Object annotation) {
        return this.pageByAnnotation(null, pageNum, pageSize, annotation);
    }

    
    default <R extends T> Page<R> pageBySorter(R model, int pageNum, int pageSize, QuerySorter sorter) {
        QueryConfig config = QueryConfig.of().addSorter(sorter);
        return this.pageByCondition(model, pageNum, pageSize, config);
    }

    
    default <R extends T> Page<R> pageBySorter(int pageNum, int pageSize, QuerySorter sorter) {
        return this.pageBySorter(null, pageNum, pageSize, sorter);
    }

    
    long countByCondition(T model, Conditions... conditions);

    
    default long countByCondition(Conditions conditions) {
        return this.countByCondition(null, conditions);
    }

    
    default long countByAnnotation(T model, Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.countByCondition(model, conditions);
    }

    
    default long countByAnnotation(Object annotation) {
        return this.countByAnnotation(null, annotation);
    }

    
    default boolean existByCondition(T model, Conditions... conditions) {
        long num = this.countByCondition(model, conditions);
        return num > 0;
    }

    
    default boolean existByCondition(Conditions conditions) {
        return this.existByCondition(null, conditions);
    }

    
    default boolean existByAnnotation(T model, Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.existByCondition(model, conditions);
    }

   
    default boolean existByAnnotation(Object annotation) {
        return this.existByAnnotation(null, annotation);
    }

}
