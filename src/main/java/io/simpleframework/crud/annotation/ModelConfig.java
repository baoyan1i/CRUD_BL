package io.simpleframework.crud.annotation;

import io.simpleframework.crud.core.DatasourceType;
import io.simpleframework.crud.core.NameType;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface ModelConfig {

    
    DatasourceType datasourceType();

    
    String datasourceName() default "";

    
    NameType tableNameType() default NameType.CLASS_DEFINED;

    
    NameType columnNameType() default NameType.CLASS_DEFINED;
}
