package io.frebel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains the <class name -> FrebelClass> mapping
 */
public class FrebelClassRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelClassRegistry.class);

    private static final Map<String, FrebelClass> frebelClassMap = new ConcurrentHashMap<>();

    /**
     * Add <class name -> FrebelClass> mapping entry in registry
     *
     * @param className the name of class, may be frebel generate class
     * @param frebelClass class wrapper of Frebel
     */
    public static void register(String className, FrebelClass frebelClass) {
        FrebelClass oldClass = frebelClassMap.put(className, frebelClass);
        if (oldClass != null) {
            LOGGER.warn("FrebelClass register multi times!, class name: {}.", className);
        } else {
            LOGGER.info("Register new FrebelClass, class name: {}", className);
        }
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

    /**
     * Test the two class is in same FrebelClass
     *
     * @param className1 one class name
     * @param className2 another class name
     * @return Is the two class is in same FrebelClass
     */
    public static boolean isSameFrebelClass(String className1, String className2) {
        FrebelClass frebelClass1 = getFrebelClass(className1);
        FrebelClass frebelClass2 = getFrebelClass(className2);
        return frebelClass2 != null && frebelClass1 == frebelClass2;
    }

    public static boolean isSameFrebelClassByName(String className1, String className2) {
        className1 = className1.replace(".", "/");
        className2 = className2.replace(".", "/");
        String classNameWithoutFrebelSuffix1 = className1.split("_\\$fr\\$")[0];
        String classNameWithoutFrebelSuffix2 = className2.split("_\\$fr\\$")[0];
        return Objects.equals(classNameWithoutFrebelSuffix1,
                classNameWithoutFrebelSuffix2);
    }
}
