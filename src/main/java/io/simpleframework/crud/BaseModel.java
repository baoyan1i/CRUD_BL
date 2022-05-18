package io.simpleframework.crud;

import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.Page;
import io.simpleframework.crud.core.QueryConfig;
import io.simpleframework.crud.core.QuerySorter;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


@SuppressWarnings("unchecked")
public interface BaseModel<T> extends Serializable {

    default boolean save() {
        boolean saveSuccess = false;
        if (this.idValue() != null) {
            saveSuccess = this.updateById();
        }
        if (!saveSuccess) {
            saveSuccess = this.insert();
        }
        return saveSuccess;
    }

    
    default boolean insert() {
        return this.mapper().insert((T) this);
    }

    
    default boolean batchInsert(List<? extends T> models) {
        return this.mapper().batchInsert(models);
    }

    
    default boolean deleteById(Serializable id) {
        return this.mapper().deleteById(id);
    }

    
    default boolean deleteByIds(Collection<? extends Serializable> ids) {
        return this.mapper().deleteByIds(ids);
    }

    
    default int deleteByConditions(Conditions conditions) {
        return this.mapper().deleteByConditions(conditions);
    }

    
    default int deleteByAnnotation(Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.deleteByConditions(conditions);
    }

    
    default boolean updateById() {
        return this.mapper().updateById((T) this);
    }

    
    default boolean updateByIdWithNull() {
        return this.mapper().updateByIdWithNull((T) this);
    }

    
    default int updateByConditions(Conditions conditions) {
        return this.mapper().updateByConditions((T) this, conditions);
    }

    
    default int updateByAnnotation(Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.updateByConditions(conditions);
    }

    
    default <R extends T> R findById(Serializable id) {
        return this.mapper().findById(id);
    }

    
    default <R extends T> List<R> listByIds(Collection<? extends Serializable> ids) {
        return this.mapper().listByIds(ids);
    }

    
    default <R extends T> R findByCondition(QueryConfig... configs) {
        List<R> result = this.listByCondition(configs);
        return result.isEmpty() ? null : result.get(0);
    }

   
    default <R extends T> List<R> listByCondition(QueryConfig... configs) {
        return this.mapper().listByCondition((R) this, configs);
    }

    
    default <R extends T> List<R> listByAnnotation(Object annotation) {
        QueryConfig config = QueryConfig.fromAnnotation(annotation);
        return this.listByCondition(config);
    }

    
    default <R extends T> List<R> listBySorter(QuerySorter sorter) {
        QueryConfig config = QueryConfig.of().addSorter(sorter);
        return this.listByCondition(config);
    }

    
    default <R extends T> Page<R> pageByCondition(int pageNum, int pageSize, QueryConfig... configs) {
        return this.mapper().pageByCondition((R) this, pageNum, pageSize, configs);
    }

    
    default <R extends T> Page<R> pageByAnnotation(int pageNum, int pageSize, Object annotation) {
        QueryConfig config = QueryConfig.fromAnnotation(annotation);
        return this.pageByCondition(pageNum, pageSize, config);
    }

    
    default <R extends T> Page<R> pageBySorter(int pageNum, int pageSize, QuerySorter sorter) {
        QueryConfig config = QueryConfig.of().addSorter(sorter);
        return this.pageByCondition(pageNum, pageSize, config);
    }

    
    default long countByCondition(Conditions... conditions) {
        return this.mapper().countByCondition((T) this, conditions);
    }

    
    default long countByAnnotation(Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.countByCondition(conditions);
    }

    
    default boolean existByCondition(Conditions... conditions) {
        long num = this.countByCondition(conditions);
        return num > 0;
    }

    
    default boolean existByAnnotation(Object annotation) {
        Conditions conditions = Conditions.fromAnnotation(annotation);
        return this.existByCondition(conditions);
    }

    
    default <E extends Serializable> E idValue() {
        ModelInfo<T> info = this.info();
        if (info == null) {
            return null;
        }
        ModelField id = info.id();
        if (id == null) {
            return null;
        }
        return (E) id.getValue((T) this);
    }

    
    default BaseModelMapper<T> mapper() {
        return Models.mapper(getClass());
    }

    
    default ModelInfo<T> info() {
        return Models.info(getClass());
    }

}
