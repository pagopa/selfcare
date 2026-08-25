package it.pagopa.selfcare.commons.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.Map;

/**
 * The Class ReflectionUtils.
 */
public class ReflectionUtils {

    /**
     * Sets the field value.
     *
     * @param object the object
     * @param field  the field
     * @param value  the value
     * @return true, if successful
     * @throws IllegalAccessException the illegal access exception
     */
    public boolean setFieldValue(Object object, Field field, Object value) throws IllegalAccessException {
        try {
            if (field != null && object != null && value != null) {
                field.set(object, value);
                return true;
            } else {
                return false;
            }
        } catch (Exception var5) {
            return false;
        }
    }

    /**
     * Gets the field.
     *
     * @param name  the name
     * @param clazz the clazz
     * @return the field
     */
    public Field getField(String name, Class<?> clazz) {
        if (name != null && clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception var4) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * New instance.
     *
     * @param clazz the clazz
     * @return the object
     */
    public Object newInstance(Class<?> clazz) {
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (Exception var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Checks if is instance of.
     *
     * @param aClassOrigin the a class origin
     * @param aClass       the a class
     * @return true, if is instance of
     */
    public boolean isInstanceOf(Class<?> aClassOrigin, Class<?> aClass) {
        boolean isInstanceOf;
        for (isInstanceOf = false; !aClassOrigin.equals(Object.class); aClassOrigin = aClassOrigin.getSuperclass()) {
            if (aClassOrigin.equals(aClass)) {
                isInstanceOf = true;
                break;
            }
        }

        return isInstanceOf;
    }

    /**
     * Gets the generic type class.
     *
     * @param myclass      the myclass
     * @param genericIndex the generic index
     * @return the generic type class
     */
    public static Object getGenericTypeClass(Class<?> myclass, int genericIndex) {
        try {
            Type actualType = ((ParameterizedType) myclass.getGenericSuperclass()).getActualTypeArguments()[genericIndex];
            if (actualType instanceof Class) {
                return actualType;
            } else if (actualType instanceof TypeVariable) {
                return Object.class;
            } else if (actualType instanceof ParameterizedType) {
                return actualType;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Exception var3) {
            throw new IllegalStateException("Class is not parametrized with generic type!!! Please use extends <> ");
        }
    }

    /**
     * It will transform the input enum into a String, using the same logic of Jackson
     */
    public static String enum2String(Enum<?> o) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Object result = enum2Object(o);

        return result != null ? result.toString() : null;
    }

    /**
     * It will transform the input enum into a BigInteger, using the same logic of Jackson
     */
    public static BigInteger enum2BigInteger(Enum<?> o) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Object result = enum2Object(o);

        return result != null ? new BigInteger(result.toString()) : null;
    }

    /**
     * It will transform the input enum into an Object, using the same logic of Jackson
     */
    private static Object enum2Object(Enum<?> o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method jsonValueMethod = null;
        for (Method m : o.getClass().getMethods()) {
            if (m.getAnnotation(JsonValue.class) != null) {
                jsonValueMethod = m;
                break;
            }
        }

        if (jsonValueMethod == null) {
            jsonValueMethod = o.getClass().getMethod("toString");
        }

        return jsonValueMethod.invoke(o);
    }

    /**
     * It will transform the input string into an Enum, using the same logic of Jackson
     */
    public static <T extends Enum<?>> T string2Enum(String o, Class<T> enumClass) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method enumCreatorMethod = null;
        for (Method m : enumClass.getMethods()) {
            if (m.getAnnotation(JsonCreator.class) != null) {
                enumCreatorMethod = m;
                break;
            }
        }

        if (enumCreatorMethod == null) {
            enumCreatorMethod = enumClass.getMethod("valueOf", String.class);
        }

        //noinspection unchecked
        return (T) enumCreatorMethod.invoke(null, o);
    }

    private static final Map<Class<?>, Class<?>> primitiveWrapperMap;

    static {
        primitiveWrapperMap = Map.of(
                boolean.class, Boolean.class,
                byte.class, Byte.class,
                char.class, Character.class,
                double.class, Double.class,
                float.class, Float.class,
                int.class, Integer.class,
                long.class, Long.class,
                short.class, Short.class);
    }

    /**
     * if the provided targetClass is the wrapper of the primitive provided
     */
    public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
        if (!primitive.isPrimitive()) {
            throw new IllegalArgumentException("The second argument has to be primitive type");
        }
        return primitiveWrapperMap.get(primitive) == targetClass;
    }
}
