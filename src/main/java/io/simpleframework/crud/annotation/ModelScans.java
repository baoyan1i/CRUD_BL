package io.simpleframework.crud.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ModelScannerRegistrar.RepeatingRegistrar.class)
public @interface ModelScans {

    ModelScan[] value();

}
