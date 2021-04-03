package io.frebel;

import io.frebel.util.FrebelUnsafe;
import io.frebel.util.PrimitiveTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FrebelObjectManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelObjectManager.class);

    private static final Map<String/* className */, Map<String, WeakReference<Object>>> objectMap =
            new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanThreadPool =
            Executors.newScheduledThreadPool(1);

    static {
        cleanThreadPool.scheduleWithFixedDelay(() -> {
            Iterator<Map<String, WeakReference<Object>>> iterator = objectMap.values().iterator();
            while (iterator.hasNext()) {
                Map<String, WeakReference<Object>> map = iterator.next();
                map.entrySet().removeIf(entry -> entry.getValue() == null);
                if (map.isEmpty()) {
                    iterator.remove();
                }
            }
        }, 1000L, 1000L, TimeUnit.MILLISECONDS);
        LOGGER.info("FrebelObjectManager clean thread has started.");
    }

    public static void register(String uuid, Object o) {
        if (uuid == null) {
            return;
        }
        Map<String, WeakReference<Object>> map = objectMap.get(uuid);
        if (map == null) {
            objectMap.put(uuid, new ConcurrentHashMap<>());
            map = objectMap.get(uuid);
        }
        map.put(o.getClass().getName(), new WeakReference<>(o));
        LOGGER.debug("uuid: {}, class name: {} registered in FrebelObjectManager", uuid, o.getClass().getName());
    }

    public static Object getSpecificVersionObject(String uuid, String className) {
        Map<String, WeakReference<Object>> treeMap = objectMap.get(uuid);
        if (treeMap == null || !treeMap.containsKey(className)) {
            return null;
        } else {
            return treeMap.get(className).get();
        }
    }

    public static Object createObjectOf(Object src, String className) {
        try {
            String uid = getUid(src);
            Map<String, WeakReference<Object>> treeMap = objectMap.get(uid);
            Object newInstance = FrebelUnsafe.getUnsafe().allocateInstance(Class.forName(className));
            Field fr$_uid = newInstance.getClass().getDeclaredField("_$fr$_uid");
            fr$_uid.setAccessible(true);
            fr$_uid.set(newInstance, uid);
            if (treeMap == null) {
                objectMap.put(uid, new HashMap<>());
                treeMap = objectMap.get(uid);
            }
            treeMap.put(className, new WeakReference<>(newInstance));
            return newInstance;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static String getUid(Object src) {
        Method method;
        try {
            method = src.getClass().getMethod("_$fr$_getUid");
            return (String) method.invoke(src);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Object createUninitializedObject(Class<?> c) {
        try {
            return FrebelUnsafe.getUnsafe().allocateInstance(c);
        } catch (InstantiationException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void copyState(Object src, Object target) throws Exception {
        Object latestVersionObject = getLatestVersionObject(target.getClass().getName(), getUid(src));
        src = latestVersionObject == null ? src : latestVersionObject;
        Set<Field> srcFields = getAllFields(src);
        Set<Field> targetFields = getAllFields(target);
        for (Field srcField : srcFields) {
            for (Field targetField : targetFields) {
                if (srcField == targetField) {
                    targetField.set(target, srcField.get(src));
                    break;
                } else if (srcField.getName().equals(targetField.getName()) &&
                        srcField.getType() == targetField.getType()) {
                    targetField.set(target, srcField.get(src));
                    break;
                }
            }
        }
    }

    private static Object getLatestVersionObject(String newClassName, String uid) {
        if (newClassName.contains("_$fr$")) {
            String previousClassName = FrebelClass.getPreviousClassName(newClassName);
            return getSpecificVersionObject(uid, previousClassName);
        } else {
            LOGGER.error("class name must have _$fr$");
            throw new RuntimeException("class name must have _$fr$");
        }
    }

    public static void clearState(Object o) throws Exception {
        Set<Field> allFields = getAllFields(o);
        for (Field field : allFields) {
            if (field.getType().isPrimitive()) {
                field.set(o, PrimitiveTypeUtil.getWrappedPrimitiveZeroValue(field.getType()));
            } else if (!field.getName().equals("_$fr$_uid")) {
                field.set(o, null);
            }
        }
    }

    private static Set<Field> getAllFields(Object object) {
        Field[] fields = object.getClass().getFields();
        for (Field field : fields) {
            field.setAccessible(true);
        }
        Field[] declaredFields = object.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
        }
        Set<Field> result = new HashSet<>(Arrays.asList(fields));
        Collections.addAll(result, declaredFields);
        return result;
    }

}
