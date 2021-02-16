package io.frebel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

// TODO weak reference
public class FrebelObjectManager {
    private static Map<String/* className */, Map<String, Object>> objectMap =
            Collections.synchronizedMap(new WeakHashMap<String, Map<String, Object>>());
    public static void register(String uuid, Object o) {
        Map<String, Object> map = objectMap.get(uuid);
        if (map == null) {
            objectMap.put(uuid, new HashMap<String, Object>());
            map = objectMap.get(uuid);
        }
        map.put(o.getClass().getName(), o);
    }

//    public static Object getObject(String uuid) {
//        TreeMap<String, Object> treeMap = objectMap.get(uuid);
//        if (treeMap == null) {
//            return null;
//        } else {
//            return treeMap.lastEntry().getValue();
//        }
//    }

    public static Object getSpecificVersionObject(String uuid, String className) {
        Map<String, Object> treeMap = objectMap.get(uuid);
        if (treeMap == null || !treeMap.containsKey(className)) {
//            System.out.printf("warn: no uuid: {%s} of class name: {%s}", uuid, className);
            return null;
        } else {
            return treeMap.get(className);
        }
    }

    public static Object createObjectOf(Object src, String className) {
        try {
            String uid = getUid(src);
            Map<String, Object> treeMap = objectMap.get(uid);
            Object newInstance = Class.forName(className).newInstance();
            Field fr$_uid = newInstance.getClass().getDeclaredField("_$fr$_uid");
            fr$_uid.setAccessible(true);
            fr$_uid.set(newInstance, uid);
            if (treeMap == null) {
                objectMap.put(uid, new HashMap<String, Object>());
                treeMap = objectMap.get(uid);
            }
            treeMap.put(className, newInstance);
            return newInstance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getUid(Object src) {
        Method method;
        try {
            method = src.getClass().getMethod("_$fr$_getUid");
            String uid = (String) method.invoke(src);
            return uid;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
