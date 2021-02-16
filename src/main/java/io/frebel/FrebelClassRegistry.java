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

    public static boolean isSameFrebelClass(String className1, String className2) {
        FrebelClass frebelClass1 = getFrebelClass(className1);
        FrebelClass frebelClass2 = getFrebelClass(className2);
        return frebelClass2 != null && frebelClass1 == frebelClass2;
    }
}
