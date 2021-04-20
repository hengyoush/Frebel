package io.frebel.util;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PrimitiveTypeUtil {

    public static boolean isPrimitive(String className) {
        try {
            return ClassPool.getDefault().get(className).isPrimitive();
        } catch (NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Class getBoxedClass(String primitiveClassName) {
        switch (primitiveClassName) {
            case "boolean": {
                return Boolean.class;
            }
            case "byte": {
                return Byte.class;
            }
            case "short": {
                return Short.class;
            }
            case "int": {
                return Integer.class;
            }
            case "long": {
                return Long.class;
            }
            case "float": {
                return Float.class;
            }
            case "double": {
                return Double.class;
            }
            case "char": {
                return Character.class;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    public static Class getPrimitiveClass(String primitiveClassName) {
        switch (primitiveClassName) {
            case "boolean": {
                return Boolean.TYPE;
            }
            case "byte": {
                return Byte.TYPE;
            }
            case "short": {
                return Short.TYPE;
            }
            case "int": {
                return Integer.TYPE;
            }
            case "long": {
                return Long.TYPE;
            }
            case "float": {
                return Float.TYPE;
            }
            case "double": {
                return Double.TYPE;
            }
            case "char": {
                return Character.TYPE;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    public static String getUnBoxedMethodSignature(String primitiveClassName, String methodName) {
        try {
            CtMethod method = ClassPool.getDefault().get(primitiveClassName.replace("/", ".")).getDeclaredMethod(methodName);
            return method.getSignature();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getPrimitiveClassNameFromBoxClass(String boxClassName) {
        switch (boxClassName) {
            case ("java.lang.Integer"): {
                return "int";
            }
            case ("java.lang.Boolean"): {
                return "boolean";
            }
            case ("java.lang.Byte"): {
                return "byte";
            }
            case ("java.lang.Short"): {
                return "short";
            }
            case ("java.lang.Long"): {
                return "long";
            }
            case ("java.lang.Double"): {
                return "double";
            }
            case ("java.lang.Float"): {
                return "float";
            }
            case ("java.lang.Character"): {
                return "char";
            }
            default:{
                throw new IllegalArgumentException();
            }
        }
    }

    public static Object getWrappedPrimitiveZeroValue(Class c) {
        switch (c.getName()) {
            case "boolean": {
                return Boolean.FALSE;
            }
            case "byte": {
                return (byte) 0;
            }
            case "short": {
                return (short) 0;
            }
            case "int": {
                return 0;
            }
            case "long": {
                return 0L;
            }
            case "float": {
                return 0f;
            }
            case "double": {
                return 0.0;
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }
}
