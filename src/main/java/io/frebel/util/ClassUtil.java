package io.frebel.util;

public class ClassUtil {

    public static final String FREBEL_GEN_CLASS_SUFFIX = "_$fr$";

    public static boolean isJdkInternalClass(String className) {
        try {
            Class<?> cls = Class.forName(className);
            if (cls.getClassLoader() == null) {
                // bootstrap classloader
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return className.startsWith("java") || className.startsWith("sun") || className.startsWith("jdk");
    }

    public static boolean isFrebelGeneratedClass(String className) {
        return className.contains(FREBEL_GEN_CLASS_SUFFIX);
    }
}
