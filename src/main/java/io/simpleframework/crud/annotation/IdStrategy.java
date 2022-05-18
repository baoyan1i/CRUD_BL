package io.simpleframework.crud.annotation;

import io.simpleframework.crud.core.IdType;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface IdStrategy {

    
    long SNOWFLAKE_BEGIN_TIME = 1609430400000L;

    
    IdType type() default IdType.SNOWFLAKE;

    
    long beginTime() default SNOWFLAKE_BEGIN_TIME;

}
