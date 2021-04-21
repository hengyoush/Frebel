package io.frebel;

import io.frebel.util.FrebelUnsafe;
import io.frebel.util.PrimitiveTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FrebelObjectManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelObjectManager.class);

    private static final Map<String/* uuid */, Map<String, Reference<Object>>> objectMap =
            new ConcurrentHashMap<>();
    private static final Map<String/* uuid */, Object> latestObjectMap = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanThreadPool =
            Executors.newScheduledThreadPool(1);
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    static {
        cleanThreadPool.scheduleWithFixedDelay(() -> {

            while (true) {
                Reference<?> reference = queue.poll();
                if (reference == null) {
                    break;
                } else {
                    Object o = reference.get();
                    String uid = getUid(o);
                    if (uid != null) {
                        LOGGER.warn("FrebelObjectManager clean GCed object, uuid:{},className:{}",
                                uid, o.getClass().getName());
                        latestObjectMap.remove(uid);
                    }
                }
            }
            Iterator<Map<String, Reference<Object>>> iterator = objectMap.values().iterator();
            while (iterator.hasNext()) {
                Map<String, Reference<Object>> map = iterator.next();
                map.entrySet().removeIf(entry -> {
                    boolean res = entry.getValue() == null;
                    if (res) {
                        LOGGER.warn("FrebelObjectManager clean thread has cleaned an entry, className:{}",
                                entry.getKey());
                    }
                    return res;
                });
                if (map.isEmpty()) {
                    iterator.remove();
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        LOGGER.info("FrebelObjectManager clean thread has started.");
    }

    public static void register(String uuid, Object o) {
        if (uuid == null) {
            return;
        }
        Map<String, Reference<Object>> map = objectMap.get(uuid);
        if (map == null) {
            objectMap.put(uuid, new ConcurrentHashMap<>());
            map = objectMap.get(uuid);
        }
        map.put(o.getClass().getName(), new WeakReference<>(o, queue));
        latestObjectMap.put(uuid, o);
    }

    public static Object getSpecificVersionObject(String uuid, String className) {
        Map<String, Reference<Object>> treeMap = objectMap.get(uuid);
        if (treeMap == null || !treeMap.containsKey(className)) {
            return null;
        } else {
            Object res = treeMap.get(className).get();
            if (res == null) {
                Object o = latestObjectMap.get(uuid);
                if (o.getClass().getName().equals(className)) {
                    LOGGER.warn("getSpecificVersionObject from latestObjectMap,uid:{},className:{}",
                            uuid, className);
                    return o;
                } else {
                    LOGGER.warn("getSpecificVersionObject has find but has been recycled,uid:{},className:{}",
                            uuid, className);
                }
            }
            return res;
        }
    }

    public static Object createObjectOf(Object src, String className) {
        try {
            String uid = getUid(src);
            Map<String, Reference<Object>> treeMap = objectMap.get(uid);
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
        if (src == null) {
            return null;
        }
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

    public static Object getLatestVersionObject(String newClassName, String uid) {
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
