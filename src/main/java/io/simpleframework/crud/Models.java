package io.simpleframework.crud;

import io.simpleframework.crud.annotation.SkipRegister;
import io.simpleframework.crud.core.ModelConfiguration;
import io.simpleframework.crud.info.clazz.ClassModelInfo;
import io.simpleframework.crud.util.SimpleCrudUtils;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("all")
public final class Models {
    private static final Map<String, ModelInfo> NAME_MODEL_INFO_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class, ModelInfo> CLASS_MODEL_INFO_CACHE = new ConcurrentHashMap<>();
    private static ModelDatasourceProvider datasourceProvider;

    
    public static <T> BaseModelMapper<T> mapper(Class<?> clazz) {
        return mapper(clazz, true);
    }

    
    public static <T> BaseModelMapper<T> mapper(Class<?> clazz, boolean registerIfAbsent) {
        ModelInfo info = info(clazz, registerIfAbsent);
        if (info == null) {
            return null;
        }
        return info.mapper(clazz);
    }

    
    public static <T> BaseModelMapper<T> mapper(String modelName) {
        ModelInfo info = info(modelName);
        if (info == null) {
            return null;
        }
        return info.mapper();
    }

    
    public static <T> ModelInfo<T> info(Class<?> clazz) {
        return info(clazz, true);
    }

    
    public static <T> ModelInfo<T> info(Class<?> clazz, boolean registerIfAbsent) {
        clazz = SimpleCrudUtils.getTargetClass(clazz);
        ModelInfo result = CLASS_MODEL_INFO_CACHE.get(clazz);
        if (result == null && registerIfAbsent) {
            register(clazz);
            result = CLASS_MODEL_INFO_CACHE.get(clazz);
        }
        if (result == null) {
            result = SimpleCrudUtils.getFromSuperclass(clazz, c -> CLASS_MODEL_INFO_CACHE.get(c));
            if (result != null) {
                CLASS_MODEL_INFO_CACHE.put(clazz, result);
            }
        }
        return result;
    }

    
    public static <T extends ModelInfo> T info(String modelName) {
        return (T) NAME_MODEL_INFO_CACHE.get(modelName);
    }

    
    public synchronized static Class register(Class<?> modelClass) {
        return register(modelClass, Object.class, ModelConfiguration.DEFAULT_CONFIG);
    }

    
    public synchronized static Class register(Class<?> modelClass, Class<?> topClass, ModelConfiguration modelConfig) {
        modelClass = SimpleCrudUtils.getTargetClass(modelClass);
        if (modelClass == null || modelClass == Object.class || modelClass == topClass) {
            return null;
        }
        if (CLASS_MODEL_INFO_CACHE.containsKey(modelClass)) {
            return modelClass;
        }
        Class registeredClass = register(modelClass.getSuperclass(), topClass, modelConfig);
        if (registeredClass != null) {
            return registeredClass;
        }
        if (modelClass.isAnnotationPresent(SkipRegister.class)) {
            return null;
        }
        ModelInfo<?> modelInfo = createInfo(modelClass, modelConfig);
        if (modelInfo == null) {
            return null;
        }
        register(modelInfo);
        return modelClass;
    }

    
    public synchronized static void register(ModelInfo info) {
        Class modelClass = info.modelClass();
        if (info.getAllFields().isEmpty()) {
            throw new IllegalArgumentException("Can not find any fields from " + modelClass + "(" + info.name() + ")");
        }
        NAME_MODEL_INFO_CACHE.put(info.name(), info);
        if (modelClass != Map.class) {
            CLASS_MODEL_INFO_CACHE.put(modelClass, info);
        }
    }

    
    public static ModelInfo<?> createInfo(Class<?> modelClass, ModelConfiguration modelConfig) {
        int classModifiers = modelClass.getModifiers();
        if (Modifier.isFinal(classModifiers)
                || Modifier.isAbstract(classModifiers)
                || Modifier.isInterface(classModifiers)
                || Map.class.isAssignableFrom(modelClass)
                || Iterable.class.isAssignableFrom(modelClass)
                || modelClass.getName().startsWith("java.")) {
            return null;
        }
        modelConfig = ModelConfiguration.fromClass(modelClass, modelConfig);
        return new ClassModelInfo<>(modelClass, modelConfig);
    }

    
    public static ModelDatasourceProvider datasourceProvider() {
        return datasourceProvider;
    }

    
    public static void datasourceProvider(ModelDatasourceProvider provider) {
        datasourceProvider = provider;
    }

}
