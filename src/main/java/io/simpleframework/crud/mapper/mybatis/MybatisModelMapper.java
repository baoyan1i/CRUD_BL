package io.simpleframework.crud.mapper.mybatis;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.Models;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.Page;
import io.simpleframework.crud.core.QueryConfig;
import io.simpleframework.crud.mapper.AbstractModelMapper;
import io.simpleframework.crud.util.SimpleCrudUtils;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static org.apache.ibatis.mapping.SqlCommandType.*;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public class MybatisModelMapper<T> extends AbstractModelMapper<T> {
    private final String namespace;
    private final Class<? extends T> modelClass;
    private final SqlSourceProvider sqlSourceProvider;

    public MybatisModelMapper(Class<? extends T> modelClass, ModelInfo<T> modelInfo) {
        super(modelInfo);
        boolean dynamicModel = Map.class.isAssignableFrom(modelClass);
        this.namespace = dynamicModel ? modelInfo.name() + System.nanoTime() : modelClass.getName();
        this.modelClass = modelClass;
        this.sqlSourceProvider = new SqlSourceProvider(modelInfo, dynamicModel);
    }

    @Override
    public boolean insert(T model) {
        if (model == null) {
            return false;
        }
        super.setIdValueIfAbsent(model);
        String msId = this.mappedStatement("insert",
                INSERT, Integer.class, sqlSourceProvider::insert);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.insert(msId, model) == 1;
            }
        } else {
            return this.sqlSession().insert(msId, model) == 1;
        }
    }

    @Override
    public boolean batchInsert(List<? extends T> models) {
        if (models == null || models.isEmpty()) {
            return false;
        }
        super.setIdValueIfAbsent(models);
        String msId = this.mappedStatement("batchInsert",
                INSERT, Integer.class, sqlSourceProvider::batchInsert);
        Map<String, Object> param = new HashMap<>(3);
        param.put("list", models);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.insert(msId, param) > 0;
            }
        } else {
            return this.sqlSession().insert(msId, param) > 0;
        }
    }

    @Override
    public boolean deleteById(Serializable id) {
        if (id == null) {
            return false;
        }
        String msId = this.mappedStatement("deleteById",
                DELETE, Integer.class, sqlSourceProvider::deleteById);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.delete(msId, id) == 1;
            }
        } else {
            return this.sqlSession().delete(msId, id) == 1;
        }
    }

    @Override
    public boolean deleteByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        String msId = this.mappedStatement("deleteByIds",
                DELETE, Integer.class, sqlSourceProvider::deleteByIds);
        Map<String, Object> param = new HashMap<>(3);
        param.put("ids", ids);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.delete(msId, param) > 0;
            }
        } else {
            return this.sqlSession().delete(msId, param) > 0;
        }
    }

    @Override
    public int deleteByConditions(Conditions conditions) {
        if (conditions == null || conditions.getConditions().isEmpty()) {
            throw new IllegalArgumentException("不支持无条件删除");
        }
        String msId = this.mappedStatement("deleteByConditions",
                DELETE, Integer.class, sqlSourceProvider::deleteByConditions);
        Map<String, Object> param = new HashMap<>(3);
        param.put("config", conditions);
        param.put("data", conditions.getConditionData());

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.delete(msId, param);
            }
        } else {
            return this.sqlSession().delete(msId, param);
        }
    }

    @Override
    public boolean updateById(T model) {
        if (model == null) {
            return false;
        }
        String msId = this.mappedStatement("updateById",
                UPDATE, Integer.class, c -> sqlSourceProvider.updateById(c, false));

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.update(msId, model) == 1;
            }
        } else {
            return this.sqlSession().update(msId, model) == 1;
        }
    }

    @Override
    public boolean updateByIdWithNull(T model) {
        if (model == null) {
            return false;
        }
        String msId = this.mappedStatement("updateByIdWithNull",
                UPDATE, Integer.class, c -> sqlSourceProvider.updateById(c, true));

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.update(msId, model) == 1;
            }
        } else {
            return this.sqlSession().update(msId, model) == 1;
        }
    }

    @Override
    public int updateByConditions(T model, Conditions conditions) {
        if (model == null) {
            throw new IllegalArgumentException("未设置要修改的模型");
        }
        if (conditions == null || conditions.getConditions().isEmpty()) {
            throw new IllegalArgumentException("不支持无条件修改");
        }
        String msId = this.mappedStatement("updateByConditions",
                UPDATE, Integer.class, sqlSourceProvider::updateByConditions);
        Map<String, Object> param = new HashMap<>(6);
        param.put("model", model);
        param.put("config", conditions);
        param.put("data", conditions.getConditionData());

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.update(msId, param);
            }
        } else {
            return this.sqlSession().update(msId, param);
        }
    }

    @Override
    public <R extends T> R findById(Serializable id) {
        if (id == null) {
            return null;
        }
        String msId = this.mappedStatement("findById",
                SELECT, modelClass, sqlSourceProvider::findById);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.selectOne(msId, id);
            }
        } else {
            return this.sqlSession().selectOne(msId, id);
        }
    }

    @Override
    public <R extends T> List<R> listByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String msId = this.mappedStatement("listByIds",
                SELECT, modelClass, sqlSourceProvider::listByIds);
        Map<String, Object> param = new HashMap<>(3);
        param.put("ids", ids);

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.selectList(msId, param);
            }
        } else {
            return this.sqlSession().selectList(msId, param);
        }
    }

    @Override
    public <R extends T> List<R> listByCondition(R model, QueryConfig... configs) {
        return this.listByCondition(new HashMap<>(6), model, configs);
    }

    private <R extends T> List<R> listByCondition(Map<String, Object> param, R model, QueryConfig... configs) {
        QueryConfig queryConfig = super.combineQueryConfigs(configs);
        boolean dynamicQuery = model == null || configs.length > 0;
        String msId = this.mappedStatement(dynamicQuery ? "listByDynamicCondition" : "listByStaticCondition",
                SELECT, modelClass, c -> sqlSourceProvider.listByCondition(c, dynamicQuery));
        param.put("model", model);
        param.put("config", queryConfig);
        param.put("data", queryConfig.getConditions().getConditionData());

        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                return session.selectList(msId, param);
            }
        } else {
            return this.sqlSession().selectList(msId, param);
        }
    }

    @Override
    public <R extends T> Page<R> pageByCondition(R model, int pageNum, int pageSize, QueryConfig... configs) {
        QueryConfig queryConfig = super.combineQueryConfigs(configs);
        long total = this.countByCondition(model, queryConfig.getConditions());
        if (total == 0) {
            return Page.of(pageNum, pageSize, total);
        }
        if (SimpleCrudUtils.pageHelperPresent) {
            return Pages.doSelectPage(pageNum, pageSize, () -> this.listByCondition(model, queryConfig), total);
        } else if (SimpleCrudUtils.mybatisPlusPresent) {
            com.baomidou.mybatisplus.core.metadata.IPage<T> mybatisPlusPage =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize, total, false);
            Map<String, Object> param = new HashMap<>(8);
            param.put("_page", mybatisPlusPage);
            List<R> items = this.listByCondition(param, model, queryConfig);
            Page<R> result = Page.of(pageNum, pageSize, total);
            result.setItems(items);
            return result;
        }
        throw new IllegalArgumentException("查无 mybatis 分页插件，只支持 PageHelper 或 MyBatis-Plus");
    }

    @Override
    public long countByCondition(T model, Conditions... conditions) {
        boolean dynamicQuery = model == null || conditions.length > 0;
        String msId = this.mappedStatement(dynamicQuery ? "countByDynamicCondition" : "countByStaticCondition",
                SELECT, Long.class, c -> sqlSourceProvider.countByCondition(c, dynamicQuery));
        Conditions combineConditions = super.combineConditions(conditions);
        Map<String, Object> param = new HashMap<>(6);
        param.put("model", model);
        param.put("config", combineConditions);
        param.put("data", combineConditions.getConditionData());

        Long result;
        if (this.needCloseSqlSession()) {
            try (SqlSession session = this.sqlSession()) {
                result = session.selectOne(msId, param);
            }
        } else {
            result = this.sqlSession().selectOne(msId, param);
        }
        return result == null ? 0 : result;
    }

    private String mappedStatement(String methodName, SqlCommandType commandType, Class<?> resultType,
                                   Function<Configuration, SqlSource> sqlSourceFunction) {
        String msId = String.format("%s.%s.%s", this.namespace, commandType, methodName);
        Configuration configuration = this.sqlSession().getConfiguration();
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        String keyColumn = null, keyFieldName = null;
        KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
        ModelField modelId = super.getModelInfo().id();
        if (commandType == INSERT && modelId != null && !modelId.insertable()) {
            keyColumn = modelId.column();
            keyFieldName = modelId.fieldName();
            keyGenerator = Jdbc3KeyGenerator.INSTANCE;
        }
        SqlSource sqlSource = sqlSourceFunction.apply(configuration);
        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, commandType)
                .resultMaps(Collections.singletonList(
                        new ResultMap.Builder(configuration, msId, resultType, new ArrayList<>()).build()
                ))
                .keyGenerator(keyGenerator)
                .keyColumn(keyColumn)
                .keyProperty(keyFieldName)
                .build();
        configuration.addMappedStatement(ms);
        return msId;
    }

    private SqlSession sqlSession() {
        String datasourceName = super.getModelInfo().config().datasourceName();
        return Models.datasourceProvider().mybatisSqlSession(datasourceName);
    }

    private boolean needCloseSqlSession() {
        String datasourceName = super.getModelInfo().config().datasourceName();
        return Models.datasourceProvider().mybatisSqlSessionCloseable(datasourceName);
    }

}
