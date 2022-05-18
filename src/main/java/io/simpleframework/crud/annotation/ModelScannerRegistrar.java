package io.simpleframework.crud.annotation;

import io.simpleframework.crud.BaseModelMapper;
import io.simpleframework.crud.Models;
import io.simpleframework.crud.core.ModelConfiguration;
import lombok.SneakyThrows;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@SuppressWarnings("all")
public class ModelScannerRegistrar implements ImportBeanDefinitionRegistrar {

    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes mapperScanAttrs =
                AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ModelScan.class.getName()));
        if (mapperScanAttrs != null) {
            String defaultBasePackage = getDefaultBasePackage(importingClassMetadata);
            this.registerBeanDefinitions(registry, mapperScanAttrs, defaultBasePackage);
        }
    }

    @SneakyThrows
    void registerBeanDefinitions(BeanDefinitionRegistry registry, AnnotationAttributes attrs, String defaultBasePackage) {
        Class<?> superClass = attrs.getClass("superClass");
        Class[] annotationClass = attrs.getClassArray("annotationClass");
        String[] basePackages = basePackages(attrs, defaultBasePackage);
        Set<String> classNames = ClassPathModelScanner.scan(superClass, annotationClass, basePackages);

        ModelConfiguration modelConfig = modelConfig(attrs);
        ClassLoader classLoader = ModelScannerRegistrar.class.getClassLoader();
        for (String className : classNames) {
            Class modelClass = ClassUtils.forName(className, classLoader);
            Class registerClass = Models.register(modelClass, superClass, modelConfig);
            if (modelClass == registerClass) {
                registerBeanDefinition(registry, modelClass, Models.mapper(modelClass, false));
            }
        }
    }

    private static synchronized void registerBeanDefinition(BeanDefinitionRegistry registry, Class clazz, BaseModelMapper<?> mapper) {
        String beanName = "simpleCrudBaseModelMapper#" + clazz.getName();
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition(BaseModelMapper.class, () -> mapper);
        beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(BaseModelMapper.class, clazz));
        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    private static String[] basePackages(AnnotationAttributes attrs, String defaultPackage) {

        List<String> result = new ArrayList<>();
        for (String p : attrs.getStringArray("value")) {
            result.add(p);
        }
        for (String p : attrs.getStringArray("basePackages")) {
            result.add(p);
        }
        if (result.isEmpty()) {
            result.add(defaultPackage);
        }
        result = result.stream().filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        return StringUtils.tokenizeToStringArray(
                StringUtils.collectionToCommaDelimitedString(result),
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
    }

    private static ModelConfiguration modelConfig(AnnotationAttributes attrs) {
        return new ModelConfiguration(
                attrs.getEnum("datasourceType"),
                attrs.getString("datasourceName"),
                attrs.getEnum("tableNameType"),
                attrs.getEnum("columnNameType"));
    }

    static class RepeatingRegistrar extends ModelScannerRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            AnnotationAttributes mapperScansAttrs =
                    AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(ModelScans.class.getName()));
            String defaultBasePackage = getDefaultBasePackage(importingClassMetadata);
            if (mapperScansAttrs != null) {
                AnnotationAttributes[] annotations = mapperScansAttrs.getAnnotationArray("value");
                for (AnnotationAttributes annotation : annotations) {
                    super.registerBeanDefinitions(registry, annotation, defaultBasePackage);
                }
            }
        }
    }

    static class ClassPathModelScanner extends ClassPathScanningCandidateComponentProvider {

        ClassPathModelScanner(Class<?> superClass, Class<? extends Annotation>[] annotationClass) {
            super(false);
            super.addIncludeFilter(new AssignableTypeFilter(superClass));
            for (Class<? extends Annotation> clazz : annotationClass) {
                super.addIncludeFilter(new AnnotationTypeFilter(clazz));
            }
        }

        static Set<String> scan(Class<?> superClass, Class[] annotationClass, String[] packages) {
            Set<BeanDefinition> beanNames = new HashSet<>();
            ClassPathModelScanner scanner = new ClassPathModelScanner(superClass, annotationClass);
            for (String p : packages) {
                beanNames.addAll(scanner.findCandidateComponents(p));
            }
            return beanNames.stream().map(BeanDefinition::getBeanClassName).collect(Collectors.toSet());

        }
    }

}
