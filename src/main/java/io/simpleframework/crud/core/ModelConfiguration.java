package io.simpleframework.crud.core;

import io.simpleframework.crud.annotation.ModelConfig;
import io.simpleframework.crud.util.SimpleCrudUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


@Data
@Accessors(fluent = true)
@EqualsAndHashCode
public class ModelConfiguration {
    public static final ModelConfiguration DEFAULT_CONFIG = new ModelConfiguration();
    public static final ModelConfiguration DEFAULT_CONFIG_FOR_CLASS = new ModelConfiguration(DatasourceType.Mybatis, "", NameType.UNDERLINE_LOWER_CASE);

    
    private final DatasourceType datasourceType;
    
    private final String datasourceName;
    
    private final NameType tableNameType;
    
    private final NameType columnNameType;


    public ModelConfiguration() {
        this(DatasourceType.CLASS_DEFINED, "");
    }

    public ModelConfiguration(DatasourceType datasourceType, String datasourceName) {
        this(datasourceType, datasourceName, NameType.CLASS_DEFINED);
    }

    public ModelConfiguration(DatasourceType datasourceType, String datasourceName, NameType nameType) {
        this(datasourceType, datasourceName, nameType, nameType);
    }

    public ModelConfiguration(DatasourceType datasourceType, String datasourceName,
                              NameType tableNameType, NameType columnNameType) {
        this.datasourceType = datasourceType != null ? datasourceType : DatasourceType.CLASS_DEFINED;
        this.datasourceName = datasourceName != null ? datasourceName : "";
        this.tableNameType = tableNameType != null ? tableNameType : NameType.CLASS_DEFINED;
        this.columnNameType = columnNameType != null ? columnNameType : NameType.CLASS_DEFINED;
    }

    public static ModelConfiguration fromClass(Class<?> clazz, ModelConfiguration defaultConfig) {
        if (defaultConfig == null) {
            defaultConfig = DEFAULT_CONFIG;
        }
        if (!defaultConfig.hasClassDefined()) {
            return defaultConfig;
        }
        ModelConfiguration modelConfig = fromClass(clazz);
        DatasourceType datasourceType = defaultConfig.datasourceType();
        String datasourceName = defaultConfig.datasourceName();
        NameType tableNameType = defaultConfig.tableNameType();
        NameType columnNameType = defaultConfig.columnNameType();
        if (datasourceType == DatasourceType.CLASS_DEFINED) {
            if (modelConfig.datasourceType() == DatasourceType.CLASS_DEFINED) {
                datasourceType = DEFAULT_CONFIG_FOR_CLASS.datasourceType();
                datasourceName = DEFAULT_CONFIG_FOR_CLASS.datasourceName();
            } else {
                datasourceType = modelConfig.datasourceType();
                datasourceName = modelConfig.datasourceName();
            }
        }
        if (tableNameType == NameType.CLASS_DEFINED) {
            if (modelConfig.tableNameType() == NameType.CLASS_DEFINED) {
                tableNameType = DEFAULT_CONFIG_FOR_CLASS.tableNameType();
            } else {
                tableNameType = modelConfig.tableNameType();
            }
        }
        if (columnNameType == NameType.CLASS_DEFINED) {
            if (modelConfig.columnNameType() == NameType.CLASS_DEFINED) {
                columnNameType = DEFAULT_CONFIG_FOR_CLASS.columnNameType();
            } else {
                columnNameType = modelConfig.columnNameType();
            }
        }
        ModelConfiguration result = new ModelConfiguration(datasourceType, datasourceName, tableNameType, columnNameType);
        if (result.equals(DEFAULT_CONFIG)) {
            return DEFAULT_CONFIG;
        }
        if (result.equals(DEFAULT_CONFIG_FOR_CLASS)) {
            return DEFAULT_CONFIG_FOR_CLASS;
        }
        return result;
    }

    public static ModelConfiguration fromConfig(ModelConfig config) {
        if (config == null) {
            return DEFAULT_CONFIG;
        }
        return new ModelConfiguration(
                config.datasourceType(), config.datasourceName(),
                config.tableNameType(), config.columnNameType());
    }

    private static ModelConfiguration fromClass(Class<?> clazz) {
        ModelConfig config = clazz.getAnnotation(ModelConfig.class);
        if (config == null) {
            config = SimpleCrudUtils.getFromSuperclass(clazz, c -> c.getAnnotation(ModelConfig.class));
        }
        return fromConfig(config);
    }

    private boolean hasClassDefined() {
        return this.datasourceType() == DatasourceType.CLASS_DEFINED
                || this.tableNameType() == NameType.CLASS_DEFINED
                || this.columnNameType() == NameType.CLASS_DEFINED;
    }


}
