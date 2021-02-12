package io.frebel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public class FrebelRuntime {
    public static Object getCurrentVersion(Object obj) {
        try {
            Method method = obj.getClass().getMethod("_$fr$_getUid");
            String uid = (String) method.invoke(obj);
            Object currentVersion = FrebelObjectManager.getObject(uid);
            if (currentVersion == null) {
                currentVersion = obj;
            }
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(obj.getClass().getName());
            if (frebelClass.isReloaded()) {
                String newestClassName = frebelClass.getCurrentVersionClassName();
                if (!Objects.equals(currentVersion.getClass().getName(), newestClassName)) {
                    // 创建新对象 && 状态拷贝
                    Object newClassInstance = Class.forName(newestClassName).newInstance();
                    copyState(currentVersion, newClassInstance);
                    // 注册新对象
                    FrebelObjectManager.register(uid, newClassInstance);
                    clearState(currentVersion);
                    return newClassInstance;
                } else {
                    return currentVersion;
                }
            } else {
                // 该类没有修改过，所以直接返回原对象
                return obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void copyState(Object srcObj, Object targetObj) {
        try {
            Method method = srcObj.getClass().getMethod("_$fr$_getUid");
            Object uuid = method.invoke(srcObj);
            Field fr$_uid = targetObj.getClass().getDeclaredField("_$fr$_uid");
            fr$_uid.setAccessible(true);
            fr$_uid.set(targetObj, uuid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void clearState(Object object) {
        // TODO
    }

    public static Object invokeWithNoParams(Class<?> enclosingClass, String methodName, Object invokeObj) {
        try {
            Object currentVersion = getCurrentVersion(invokeObj);
            Method method = currentVersion.getClass().getMethod(methodName);
            Object returnObj = method.invoke(currentVersion);
            return returnObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeWithParams(Class<?> enclosingClass, String methodName, Object invokeObj, Object[] args, Class<?>[] argsType) {
        try {
            Object currentVersion = getCurrentVersion(invokeObj);
            Method method = currentVersion.getClass().getMethod(methodName, argsType);
            Object returnObj = method.invoke(currentVersion, args);
            return returnObj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
