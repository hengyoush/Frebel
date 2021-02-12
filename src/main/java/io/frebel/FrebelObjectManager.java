package io.frebel;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class FrebelObjectManager {
    private static Map<String/* className */, Object> objectMap = Collections.synchronizedMap(new WeakHashMap<String, Object>());

    public static void register(String uuid, Object o) {
        objectMap.put(uuid, o);
    }

    public static Object getObject(String uuid) {
        return objectMap.get(uuid);
    }
}
