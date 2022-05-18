package io.simpleframework.crud.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.simpleframework.crud.core.SerializedFunction;
import lombok.SneakyThrows;

import java.lang.invoke.SerializedLambda;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
@SuppressWarnings("all")
public final class SimpleCrudUtils {
    private static final List<String> PROXY_CLASSES = Arrays.asList(
            "net.sf.cglib.proxy.Factory",
            "org.springframework.cglib.proxy.Factory",
            "javassist.util.proxy.ProxyObject",
            "org.apache.ibatis.javassist.util.proxy.ProxyObject");
    private static final Map<Class<?>, WeakReference<SerializedLambda>> LAMBDA_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, WeakReference<Constructor>> CONSTRUCTORS = new ConcurrentHashMap<>();

    public static boolean jpaPresent;
    public static boolean pageHelperPresent;
    public static boolean mybatisPlusPresent;
    private static boolean jsonPresent;
    private static ObjectMapper objectMapper;

    static {
        try {
            Class.forName("javax.persistence.Table");
            jpaPresent = true;
        } catch (Throwable e) {
            jpaPresent = false;
        }
        try {
            Class.forName("com.github.pagehelper.PageHelper");
            pageHelperPresent = true;
        } catch (Throwable e) {
            pageHelperPresent = false;
        }
        try {
            Class.forName("com.baomidou.mybatisplus.core.metadata.IPage");
            mybatisPlusPresent = true;
        } catch (Throwable e) {
            mybatisPlusPresent = false;
        }
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            jsonPresent = true;
            objectMapper = new ObjectMapper();
        } catch (Throwable e) {
            jsonPresent = false;
        }
    }

    /**
     * 获取模型类
     *
     * @param clazz 原类
     * @return 如果是代理类，返回父类，否则返回自身
     */
    public static Class<?> getTargetClass(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass == null || superclass == Object.class) {
            return clazz;
        }
        boolean isProxy = false;
        if (clazz.getName().contains("$$")) {
            isProxy = true;
        } else {
            for (Class<?> cls : clazz.getInterfaces()) {
                if (PROXY_CLASSES.contains(cls.getName())) {
                    isProxy = true;
                    break;
                }
            }
        }
        return isProxy ? superclass : clazz;
    }

    /**
     * 字符串驼峰转下划线格式
     *
     * @param param 需要转换的字符串
     * @return 转换好的字符串
     */
    public static String camelToUnderline(String param) {
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append("_");
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 获取子类及父类的所有字段列表
     * 若覆写了父类字段，则取子类的字段
     *
     * @param clazz       类
     * @param fieldFilter 字段过滤器
     * @return 字段列表
     */
    public static List<Field> getFields(Class<?> clazz, Predicate<Field> fieldFilter) {
        List<Field> superFields = new ArrayList<>();
        Class<?> currentClass = clazz.getSuperclass();
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            Collections.addAll(superFields, declaredFields);
            currentClass = currentClass.getSuperclass();
        }
        Map<String, Field> fields = Stream.of(clazz.getDeclaredFields())
                .collect(toMap(Field::getName, f -> f, (o1, o2) -> o1, LinkedHashMap::new));
        for (Field field : superFields) {
            String fieldName = field.getName();
            if (fields.containsKey(fieldName)) {
                continue;
            }
            fields.put(fieldName, field);
        }
        return fields.values().stream()
                .filter(fieldFilter)
                .collect(toList());
    }

    /**
     * 获取表达式方法的字段名
     * 去除方法名前的 is/get/set 并将首字符转为小写
     *
     * @param function 表达式
     * @return 类字段名
     */
    public static String getLambdaFieldName(SerializedFunction<?, ?> function) {
        Class<?> funcClass = function.getClass();
        SerializedLambda lambda = Optional.ofNullable(LAMBDA_CACHE.get(funcClass))
                .map(WeakReference::get)
                .orElseGet(() -> {
                    SerializedLambda temp;
                    try {
                        Method method = funcClass.getDeclaredMethod("writeReplace");
                        method.setAccessible(Boolean.TRUE);
                        temp = (SerializedLambda) method.invoke(function);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    LAMBDA_CACHE.put(funcClass, new WeakReference<>(temp));
                    return temp;
                });
        String result = lambda.getImplMethodName();
        if (result.startsWith("is")) {
            result = result.substring(2);
        } else if (result.startsWith("get") || result.startsWith("set")) {
            result = result.substring(3);
        }
        boolean firstCharNeedToLower = result.length() == 1 ||
                result.length() > 1 && !Character.isUpperCase(result.charAt(1));
        if (firstCharNeedToLower) {
            result = result.substring(0, 1).toLowerCase(Locale.ENGLISH) + result.substring(1);
        }
        return result;
    }

    /**
     * 递归获取父类的信息
     *
     * @param clazz 要查询的类
     * @param func  获取信息的方法
     * @param <T>   方法返回值类型
     * @return func 结果
     */
    public static <T> T getFromSuperclass(Class<?> clazz, Function<Class<?>, T> func) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        T result = null;
        Class<?> supperClass = clazz.getSuperclass();
        while (result == null) {
            if (supperClass == null || supperClass == Object.class) {
                break;
            }
            result = func.apply(supperClass);
            supperClass = supperClass.getSuperclass();
        }
        return result;
    }

    /**
     * 获取空构造函数或只含 constructorParameterTypes 一个参数的构造函数
     *
     * @param clazz                     要查询的类
     * @param constructorParameterTypes 单参数类型
     * @param <T>                       要查询的类
     * @return 构造函数
     */
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?> constructorParameterTypes) {
        return Optional.ofNullable(CONSTRUCTORS.get(clazz))
                .map(WeakReference::get)
                .orElseGet(() -> {
                    Constructor<T> c = null;
                    try {
                        c = clazz.getDeclaredConstructor();
                    } catch (Exception e) {
                        try {
                            c = clazz.getDeclaredConstructor(constructorParameterTypes);
                        } catch (Exception ignore) {
                        }
                    }
                    if (c != null) {
                        c.setAccessible(true);
                        CONSTRUCTORS.put(clazz, new WeakReference<>(c));
                    }
                    return c;
                });
    }

    /**
     * 获取字段的泛型
     *
     * @param field        类字段·
     * @param defaultClass 默认结果值
     * @return 泛型
     */
    public static Class<?> getGenericClass(Field field, Class<?> defaultClass) {
        Class<?> result = defaultClass;
        Type genericType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        try {
            result = (Class<?>) genericType;
        } catch (Exception ignore) {
            try {
                result = (Class<?>) ((ParameterizedType) genericType).getRawType();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * 字符串是否有值
     * Alias for org.springframework.util.StringUtils. hasText
     *
     * @param str 字符串
     * @return 是否有值
     */
    public static boolean hasText(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0, len = str.length(); i < len; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否空白字符串
     *
     * @param str 字符串
     * @return 是否空白
     */
    public static boolean isBlank(String str) {
        return !hasText(str);
    }

    /**
     * 解析字符串，转为目标类实例
     * 1. String：原样
     * 2. Number 子类：调用参数为 string 的构造函数，例 new Integer(defaultValue)
     * 3. Boolean：调用 Boolean.valueOf(str)
     * 4. Date 及其子类：调用 new Date(long date)，即 new Date或子类(Long.parseLong(defaultValue))
     * 5. Class：调用 Class.forName(str)
     * 6. 其他类型：基于 jackson-databind 解析 json 字符串
     *
     * @param str   字符串
     * @param clazz 目标类
     * @return 目标类实例
     */
    @SneakyThrows
    public static <T> T cast(String str, Class<T> clazz) {
        if (String.class == clazz) {
            return (T) str;
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return clazz.getConstructor(String.class).newInstance(str);
        }
        if (Boolean.class == clazz || boolean.class == clazz) {
            return (T) Boolean.valueOf(str);
        }
        if (Date.class.isAssignableFrom(clazz)) {
            long date = Long.parseLong(str);
            return clazz.getConstructor(long.class).newInstance(date);
        }
        if (Class.class == clazz) {
            return (T) Class.forName(str);
        }
        return objectMapper.readValue(str, clazz);
    }

}
