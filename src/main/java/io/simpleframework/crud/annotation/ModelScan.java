package io.simpleframework.crud.annotation;

import io.simpleframework.crud.BaseModel;
import io.simpleframework.crud.core.DatasourceType;
import io.simpleframework.crud.core.NameType;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ModelScannerRegistrar.class)
@Repeatable(ModelScans.class)
public @interface ModelScan {

   
    String[] value() default {};

    
    String[] basePackages() default {};

    
    Class<?> superClass() default BaseModel.class;

    
    Class<? extends Annotation>[] annotationClass() default {};

    
    DatasourceType datasourceType() default DatasourceType.CLASS_DEFINED;

    
    String datasourceName() default "";

    
    NameType tableNameType() default NameType.CLASS_DEFINED;

    
    NameType columnNameType() default NameType.CLASS_DEFINED;

}
