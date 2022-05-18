package io.simpleframework.crud.mapper.mybatis;

import io.simpleframework.crud.ModelField;
import io.simpleframework.crud.core.Conditions;
import io.simpleframework.crud.core.QuerySorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public final class MybatisScripts {

    /**
     * 转换成 if 标签的脚本片段
     *
     * @param script script
     * @return <if test="类字段名 != null">script</if>
     */
    public static String wrapperIf(ModelField field, String script) {
        if (field.fieldType().isPrimitive()) {
            return script;
        }
        return wrapperIf(field.fieldName(), script);
    }

    /**
     * 转换成 if 标签的脚本片段
     *
     * @param script script
     * @return <if test="类字段名 != null">script</if>
     */
    public static String wrapperIf(String fieldName, String script) {
        return String.format("<if test=\"%s != null\">%s</if>", fieldName, script);
    }

    /**
     * 循环的脚本片段
     */
    public static String foreach(String collection, String item) {
        return String.format(
                "<foreach collection=\"%s\" item=\"%s\" open=\"(\" separator=\",\" close=\")\">#{%s}</foreach>",
                collection, item, item);
    }

    /**
     * 字段转为相等的脚本
     *
     * @return 表字段名 = #{类字段名}
     */
    public static String columnEqual(ModelField field) {
        return String.format("%s = #{%s}", field.column(), field.fieldName());
    }

    /**
     * 字段列表转为新增脚本
     * insert into table (字段) values (值)
     * 位于 "字段" 部位
     * <if test="...">表字段名,</if>
     * <if test="...">表字段名,</if>
     * ...
     */
    public static String insertColumnScript(List<ModelField> fields, boolean needIf) {
        String script = fields.stream()
                .map(field -> {
                    if (needIf) {
                        return MybatisScripts.wrapperIf(field, field.column() + ",");
                    } else {
                        return field.column();
                    }
                })
                .collect(Collectors.joining(needIf ? "\n" : ","));
        if (needIf) {
            return String.format("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim>", script);
        } else {
            return "(" + script + ")";
        }
    }

    /**
     * 字段列表转为新增脚本
     * insert into table (字段) values (值)
     * 位于 "值" 部位
     *
     * @param prefix 非空字符串表示批量插入脚本，不需要 if 标签
     *               #{prefix.类字段名},#{prefix.类字段名},...
     *               ---
     *               单个插入脚本，需要 if 标签
     *               <if test="...">#{prefix.类字段名},</if>
     *               <if test="...">#{prefix.类字段名},</if>
     *               ...
     */
    public static String insertFieldScript(List<ModelField> fields, boolean needIf, String prefix) {
        String script = fields.stream()
                .map(field -> {
                    String fieldScript = field.fieldName();
                    if (prefix.isEmpty()) {
                        fieldScript = String.format("#{%s}", fieldScript);
                    } else {
                        fieldScript = String.format("#{%s.%s}", prefix, fieldScript);
                    }
                    if (needIf) {
                        return MybatisScripts.wrapperIf(field, fieldScript + ",");
                    } else {
                        return fieldScript;
                    }
                })
                .collect(Collectors.joining(needIf ? "\n" : ","));
        if (needIf) {
            return String.format("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim>", script);
        } else {
            return "(" + script + ")";
        }
    }

    /**
     * SELECT 字段列表 FROM 表名
     */
    public static String selectColumnFromTable(String tableName, List<ModelField> fields) {
        String columns = fields.stream()
                .map(field -> {
                    String column = field.column();
                    String fieldName = field.fieldName();
                    return fieldName.equals(column) ?
                            column : column + " AS " + fieldName;
                })
                .collect(Collectors.joining(","));
        return String.format("SELECT %s FROM %s ", columns, tableName);
    }

    /**
     * 排序脚本
     */
    public static String sortScript(List<ModelField> fields, QuerySorter sorter) {
        Map<String, Boolean> sorterItems = sorter.getItems();
        if (sorterItems.isEmpty()) {
            return "";
        }
        Map<String, String> fieldColumns = fields.stream()
                .collect(Collectors.toMap(ModelField::fieldName, ModelField::column));
        List<String> scripts = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : sorterItems.entrySet()) {
            String column = fieldColumns.get(entry.getKey());
            if (column == null) {
                continue;
            }
            String sortType = entry.getValue() ? "asc" : "desc";
            scripts.add(column + " " + sortType);
        }
        if (scripts.isEmpty()) {
            return "";
        }
        return " \n ORDER BY " + String.join(",", scripts);
    }

    public static String conditionScript(List<ModelField> fields) {
        return conditionScript(fields, Conditions.of(), true);
    }

    public static String conditionScript(List<ModelField> fields, Conditions conditions, boolean hasModelCondition) {
        List<String> scripts = new ArrayList<>();
        for (ModelField field : fields) {
            List<Conditions.ConditionInfo> fieldConditions = conditions.getConditionInfos(field.fieldName(), hasModelCondition);
            for (Conditions.ConditionInfo condition : fieldConditions) {
                scripts.add(CONDITION_SCRIPT_PROVIDER.apply(field, condition));
            }
        }
        if (scripts.isEmpty()) {
            return "";
        }
        String result = String.join("\n", scripts);
        return " \n<where>\n" + result + "\n</where> ";
    }

    private static final BiFunction<ModelField, Conditions.ConditionInfo, String> CONDITION_SCRIPT_PROVIDER =
            (field, condition) -> {
                String script;
                String column = field.column();
                String fieldName = field.fieldName();
                String paramKey;
                if (condition.getValue() == null) {
                    paramKey = String.format("%s.%s", "model", fieldName);
                } else {
                    paramKey = String.format("%s.%s", "data", condition.getKey(fieldName));
                }
                boolean needWrapIf = true;
                switch (condition.getType()) {
                    case equal:
                        script = String.format("%s = #{%s}", column, paramKey);
                        break;
                    case not_equal:
                        script = String.format("%s <![CDATA[ <> ]]> #{%s}", column, paramKey);
                        break;
                    case like_all:
                        script = String.format("%s LIKE concat('%%', #{%s}, '%%')", column, paramKey);
                        break;
                    case like_left:
                        script = String.format("%s LIKE concat('%%', #{%s})", column, paramKey);
                        break;
                    case like_right:
                        script = String.format("%s LIKE concat(#{%s}, '%%')", column, paramKey);
                        break;
                    case greater_than:
                        script = String.format("%s <![CDATA[ > ]]> #{%s}", column, paramKey);
                        break;
                    case great_equal:
                        script = String.format("%s <![CDATA[ >= ]]> #{%s}", column, paramKey);
                        break;
                    case less_than:
                        script = String.format("%s <![CDATA[ < ]]> #{%s}", column, paramKey);
                        break;
                    case less_equal:
                        script = String.format("%s <![CDATA[ <= ]]> #{%s}", column, paramKey);
                        break;
                    case is_null:
                        script = String.format("%s IS NULL", column);
                        needWrapIf = false;
                        break;
                    case not_null:
                        script = String.format("%s IS NOT NULL", column);
                        needWrapIf = false;
                        break;
                    case in:
                        script = String.format("%s IN %s", column, foreach(paramKey, "_" + fieldName));
                        break;
                    case not_in:
                        script = String.format("%s NOT IN %s", column, foreach(paramKey, "_" + fieldName));
                        break;
                    default:
                        throw new IllegalArgumentException("Not support conditionType [" + condition.getType() + "]");
                }
                script = " AND " + script + " ";
                if (field.fieldType().isPrimitive()) {
                    return script;
                }
                return needWrapIf ? wrapperIf(paramKey, script) : script;
            };

}
