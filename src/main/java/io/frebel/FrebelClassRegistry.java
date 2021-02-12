package io.frebel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FrebelClassRegistry {
    private static Map<String, FrebelClass> frebelClassMap = new ConcurrentHashMap<>();

    public static void register(String className, FrebelClass frebelClass) {
        frebelClassMap.put(className, frebelClass);
    }

    public static FrebelClass getFrebelClass(String className) {
        if (className.contains("/")) {
            className = className.replace("/", ".");
        }
        if (className.contains("_$fr$")) {
            className = className.split("_\\$fr\\$")[0];
        }
        return frebelClassMap.get(className);
    }
}
