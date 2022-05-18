package io.simpleframework.crud;

import org.apache.ibatis.session.SqlSession;


public interface ModelDatasourceProvider {

    
    default <T> T getBean(String name, Class<T> clazz) {
        throw new IllegalArgumentException("Can not found datasource " + clazz + " " + name);
    }

    
    default SqlSession mybatisSqlSession(String name) {
        return getBean(name, SqlSession.class);
    }

    
    default boolean mybatisSqlSessionCloseable(String name) {
        return false;
    }

}
