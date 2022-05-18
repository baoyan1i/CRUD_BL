package io.simpleframework.crud.mapper.mybatis;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.ModelInfo;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.QueryConfig;
import io.simpleframework.crud.util.SimpleCrudUtils;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@SuppressWarnings("unchecked")
public class SqlSourceProvider {
    private final boolean dynamic;
    private final ModelInfo<?> modelInfo;

    public SqlSourceProvider(ModelInfo<?> modelInfo, boolean dynamic) {
        this.dynamic = dynamic;
        this.modelInfo = modelInfo;
    }

    public SqlSource insert(Configuration config) {
        return this.buildSqlSource(config, () -> {
            List<ModelField> fields = this.modelInfo.getInsertFields();
            String columnScript = MybatisScripts.insertColumnScript(fields, true);
            String fieldScript = MybatisScripts.insertFieldScript(fields, true, "");
            return String.format("<script>INSERT INTO %s %s VALUES \n %s</script>",
                    modelInfo.name(), columnScript, fieldScript);
        });
    }

    public SqlSource batchInsert(Configuration config) {
        return this.buildSqlSource(config, () -> {
            List<ModelField> fields = this.modelInfo.getInsertFields();
            String itemPrefix = "et";
            String columnScript = MybatisScripts.insertColumnScript(fields, false);
            String fieldScript = MybatisScripts.insertFieldScript(fields, false, itemPrefix);
            fieldScript = String.format("<foreach collection=\"list\" item=\"%s\" separator=\",\">%s</foreach>", itemPrefix, fieldScript);
            return String.format("<script>INSERT INTO %s %s VALUES \n %s</script>",
                    modelInfo.name(), columnScript, fieldScript);
        });
    }

    public SqlSource deleteById(Configuration config) {
        return this.buildSqlSource(config, () -> {
            ModelField idField = this.getAndValidIdField();
            return String.format("DELETE FROM %s WHERE %s = #{id}",
                    modelInfo.name(), idField.column());
        });
    }

    public SqlSource deleteByIds(Configuration config) {
        return this.buildSqlSource(config, () -> {
            ModelField idField = this.getAndValidIdField();
            String condition = MybatisScripts.foreach("ids", "id");
            return String.format("<script>DELETE FROM %s WHERE %s IN \n %s </script>",
                    modelInfo.name(), idField.column(), condition);
        });
    }

    public SqlSource deleteByConditions(Configuration config) {
        return buildDynamicSqlSource(config, p -> {
            Map<String, Object> param = (Map<String, Object>) p;
            Conditions conditions = (Conditions) param.get("config");
            List<ModelField> conditionFields = conditions.getFields(this.modelInfo.getAllFields());
            if (conditionFields.isEmpty()) {
                throw new IllegalArgumentException("不支持无条件删除");
            }
            String condition = MybatisScripts.conditionScript(conditionFields, conditions, false);
            if (SimpleCrudUtils.isBlank(condition)) {
                throw new IllegalArgumentException("不支持无条件删除");
            }
            return String.format("<script>DELETE FROM %s %s</script>",
                    modelInfo.name(), condition);
        });
    }

    public SqlSource updateById(Configuration config, boolean updateNullField) {
        Supplier<String> scriptFunc = () -> {
            ModelField idField = this.getAndValidIdField();
            String script = modelInfo.getUpdateFields().stream()
                    .map(field -> {
                        String set = MybatisScripts.columnEqual(field) + ",";
                        return updateNullField ? set : MybatisScripts.wrapperIf(field, set);
                    })
                    .collect(Collectors.joining("\n"));
            return String.format("<script>UPDATE %s \n <set>%s</set> \n WHERE %s</script>",
                    modelInfo.name(), script, MybatisScripts.columnEqual(idField));
        };
        return this.buildSqlSource(config, scriptFunc);
    }

    public SqlSource updateByConditions(Configuration config) {
        return buildDynamicSqlSource(config, p -> {
            Map<String, Object> param = (Map<String, Object>) p;
            Conditions conditions = (Conditions) param.get("config");
            List<ModelField> conditionFields = conditions.getFields(this.modelInfo.getAllFields());
            if (conditionFields.isEmpty()) {
                throw new IllegalArgumentException("不支持无条件修改");
            }
            String script = modelInfo.getUpdateFields().stream()
                    .map(field -> {
                        String fieldName = "model." + field.fieldName();
                        String set = String.format("%s = #{%s},", field.column(), fieldName);
                        return MybatisScripts.wrapperIf(fieldName, set);
                    })
                    .collect(Collectors.joining("\n"));
            if (SimpleCrudUtils.isBlank(script)) {
                throw new IllegalArgumentException("无可修改字段");
            }
            String condition = MybatisScripts.conditionScript(conditionFields, conditions, false);
            if (SimpleCrudUtils.isBlank(condition)) {
                throw new IllegalArgumentException("不支持无条件修改");
            }
            return String.format("<script>UPDATE %s \n <set>%s</set> \n %s</script>",
                    modelInfo.name(), script, condition);
        });
    }

    public SqlSource findById(Configuration config) {
        return this.buildSqlSource(config, () -> {
            ModelField idField = this.getAndValidIdField();
            List<ModelField> fields = this.modelInfo.getAllFields();
            String script = MybatisScripts.selectColumnFromTable(modelInfo.name(), fields);
            return String.format("<script>%s WHERE %s = #{id}</script>", script, idField.column());
        });
    }

    public SqlSource listByIds(Configuration config) {
        return this.buildSqlSource(config, () -> {
            ModelField idField = this.getAndValidIdField();
            List<ModelField> fields = this.modelInfo.getAllFields();
            String script = MybatisScripts.selectColumnFromTable(modelInfo.name(), fields);
            String condition = "<foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">#{id}</foreach>";
            return String.format("<script>%s WHERE \n %s IN \n %s </script>",
                    script, idField.column(), condition);
        });
    }

    public SqlSource listByCondition(Configuration config, boolean dynamicQuery) {
        if (dynamicQuery) {
            return buildDynamicSqlSource(config, p -> {
                Map<String, Object> param = (Map<String, Object>) p;
                boolean hasModelCondition = param.get("model") != null;
                QueryConfig queryConfig = (QueryConfig) param.get("config");
                List<ModelField> allFields = this.modelInfo.getAllFields();
                String column = MybatisScripts.selectColumnFromTable(modelInfo.name(), queryConfig.getSelectFields(allFields));
                String condition = MybatisScripts.conditionScript(allFields, queryConfig.getConditions(), hasModelCondition);
                String sort = MybatisScripts.sortScript(allFields, queryConfig.getSorter());
                return "<script>" + column + condition + sort + "</script>";
            });
        } else {
            return this.buildSqlSource(config, () -> {
                List<ModelField> allFields = this.modelInfo.getAllFields();
                String column = MybatisScripts.selectColumnFromTable(modelInfo.name(), allFields);
                String condition = MybatisScripts.conditionScript(allFields);
                return "<script>" + column + condition + "</script>";
            });
        }
    }

    public SqlSource countByCondition(Configuration config, boolean dynamicQuery) {
        if (dynamicQuery) {
            return buildDynamicSqlSource(config, p -> {
                Map<String, Object> param = (Map<String, Object>) p;
                boolean hasModelCondition = param.get("model") != null;
                Conditions conditions = (Conditions) param.get("config");
                List<ModelField> fields = this.modelInfo.getAllFields();
                String condition = MybatisScripts.conditionScript(fields, conditions, hasModelCondition);
                return String.format("<script>SELECT COUNT(*) FROM %s %s</script>",
                        modelInfo.name(), condition);
            });
        } else {
            return this.buildSqlSource(config, () -> {
                List<ModelField> fields = this.modelInfo.getAllFields();
                String condition = MybatisScripts.conditionScript(fields);
                return String.format("<script>SELECT COUNT(*) FROM %s %s</script>",
                        modelInfo.name(), condition);
            });
        }
    }

    private ModelField getAndValidIdField() {
        ModelField modelId = this.modelInfo.id();
        if (modelId == null) {
            throw new IllegalArgumentException(String.format("%s 未配置 id 字段", modelInfo.name()));
        }
        return modelId;
    }

    private SqlSource buildSqlSource(Configuration config, Supplier<String> script) {
        if (this.dynamic) {
            return buildDynamicSqlSource(config, p -> script.get());
        } else {
            return config
                    .getDefaultScriptingLanguageInstance()
                    .createSqlSource(config, script.get(), null);
        }
    }

    private static SqlSource buildDynamicSqlSource(Configuration config, Function<Object, String> script) {
        return param -> config
                .getDefaultScriptingLanguageInstance()
                .createSqlSource(config, script.apply(param), null)
                .getBoundSql(param);
    }

}
