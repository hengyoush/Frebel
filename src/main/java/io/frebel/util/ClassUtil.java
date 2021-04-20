package io.frebel.util;

public class ClassUtil {

    public static final String FREBEL_GEN_CLASS_SUFFIX = "_$fr$";

    public static boolean needSkipTransform(String className) {
        if (className.startsWith("com/sun") || className.startsWith("junit")
            || className.startsWith("com/intellij") || className.contains("junit")
            || className.contains("PrimitiveWrapper")) {
            return true;
        }
        if (isFrebelGeneratedClass(className) || className.startsWith("io/frebel")) {
            return true;
        }
        try {
            Class<?> cls = Class.forName(className);
            if (cls.getClassLoader() == null) {
                // bootstrap classloader
                return true;
            }
        } catch (Throwable ignored) {

        }

        return className.startsWith("java") || className.startsWith("sun") || className.startsWith("jdk");
    }

    public static boolean isFrebelGeneratedClass(String className) {
        return className.contains(FREBEL_GEN_CLASS_SUFFIX);
    }
}
