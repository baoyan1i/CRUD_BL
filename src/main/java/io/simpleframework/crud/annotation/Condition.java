package io.simpleframework.crud.annotation;

import io.simpleframework.crud.core.ConditionType;

import java.lang.annotation.*;


@Repeatable(ConditionConfigs.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Condition {

    
    String field() default "";

    
    ConditionType type() default ConditionType.equal;

    
    String defaultValueIfNull() default "";

}
