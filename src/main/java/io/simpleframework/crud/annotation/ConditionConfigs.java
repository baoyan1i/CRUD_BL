package io.simpleframework.crud.annotation;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ConditionConfigs {

    Condition[] value();

}
