package io.frebel.util;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PrimitiveTypeUtil {
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
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    public static String getUnBoxedMethodSignature(String primitiveClassName, String methodName) {
        try {
            CtMethod method = ClassPool.getDefault().get(primitiveClassName).getDeclaredMethod(methodName);
            return method.getSignature();
        } catch (NotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
