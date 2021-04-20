package io.frebel;

import io.frebel.common.FrebelInvocationException;
import io.frebel.common.PrimitiveWrapper;
import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static io.frebel.FrebelClassRegistry.isSameFrebelClass;

@SuppressWarnings("unused")
public class FrebelRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelRuntime.class);

    public static Object getCurrentVersion(Object obj) {
        try {
            String uid = FrebelObjectManager.getUid(obj);
            if (uid == null) {
                return obj;
            }

            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(obj.getClass().getName());
            if (frebelClass == null) {
                return obj;
            }

            String currentVersionClassName = frebelClass.getCurrentVersionClassName();
            Object currentVersion = FrebelObjectManager.getSpecificVersionObject(uid, currentVersionClassName);
            if (currentVersion == null) {
                currentVersion = obj;
            }

            if (frebelClass.isReloaded()) {
                if (!Objects.equals(currentVersion.getClass().getName(), currentVersionClassName)) {
                    synchronized (obj) {
                        currentVersionClassName = frebelClass.getCurrentVersionClassName();
                        currentVersion = FrebelObjectManager.getSpecificVersionObject(uid, currentVersionClassName);
                        if (currentVersion == null) {
                            currentVersion = obj;
                        }
                        if (!Objects.equals(currentVersion.getClass().getName(), currentVersionClassName)) {
                            LOGGER.info("Current version class name: {}, newest version class name: {}",
                                    currentVersion.getClass().getName(),
                                    currentVersionClassName);
                            LOGGER.info("Start create new version object, uid: {}", uid);
                            // 创建新对象 && 状态拷贝
                            Object newClassInstance = FrebelObjectManager.createUninitializedObject(Class.forName(currentVersionClassName));
                            LOGGER.debug("Create uninitialized object finished, uid: {}", uid);
                            FrebelObjectManager.copyState(currentVersion, newClassInstance);
                            LOGGER.debug("Copy state finished, uid: {}", uid);
                            // 注册新对象
                            FrebelObjectManager.register(uid, newClassInstance);
                            LOGGER.debug("Register newer version object finished, uid: {}", uid);
                            FrebelObjectManager.clearState(currentVersion);
                            LOGGER.debug("Clear old object state finished, uid: {}", uid);
                            return newClassInstance;
                        } else {
                            return currentVersion;
                        }
                    }
                } else {
                    return currentVersion;
                }
            } else {
                // 该类没有修改过,所以直接返回原对象
                return obj;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Object getSpecificVersion(Object obj, String className) {
        try {
            String uid = FrebelObjectManager.getUid(obj);
            Object result = FrebelObjectManager.getSpecificVersionObject(uid, className);
            if (result == null) {
                return FrebelObjectManager.createObjectOf(obj, className);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return obj;
        }
    }

    public static Object invokeConsWithNoParams(String className, String descriptor, String returnTypeName) {
        className = className.replace("/", ".");
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        try {
            if (frebelClass != null) {
                // reloaded
                Object result = Class.forName(className).newInstance();
                Object currentVersion = getCurrentVersion(result);
                if (currentVersion.getClass().getName().contains("_$fr$_")) {
                    if (isSameFrebelClass(result.getClass().getName(), returnTypeName)) {
                        return getSpecificVersion(currentVersion, returnTypeName.replace("/", "."));
                    } else {
                        String matched = frebelClass.getMatchedClassNameByParentClassName(returnTypeName);
                        return getSpecificVersion(currentVersion, matched);
                    }
                } else {
                    return result;
                }
            } else {
                return Class.forName(className).newInstance();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FrebelInvocationException(e);
        }
    }

    public static Object invokeConsWithParams(String className, String descriptor, Object[] args, Class<?>[] argsType, String returnTypeName) {
        className = className.replace("/", ".");
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        try {
            if (frebelClass != null) {
                Class<?> currentVersionClass = Class.forName(frebelClass.getCurrentVersionClassName());
                Constructor<?>[] constructors = currentVersionClass.getConstructors();
                Constructor<?> matched = null;
                for (Constructor<?> constructor : constructors) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (isMatchedMethod(parameterTypes, argsType)) {
                        matched = constructor;
                        break;
                    }
                }

                if (matched != null) {
                    Object[] newArgs = new Object[args.length];
                    Class<?>[] parameterTypes = matched.getParameterTypes();
                    boolean retryFlag = false;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (parameterTypes[i].getName().equals(argsType[i].getName())) {
                            newArgs[i] = args[i];
                        } else if (isSameFrebelClass(parameterTypes[i].getName(), argsType[i].getName())) {
                            Object newArg = getCurrentVersion(args[i]);
                            if (newArg.getClass() != parameterTypes[i]) {
                                // retry because between 1 and 2 args class has benn reloaded,
                                // so the class version has been outdated
                                LOGGER.warn("Retry to find matched constructor, constructor: {}, args index: {}, method defined type: {}" +
                                        ", current version type: {}", matched, i, parameterTypes[i].getName(), newArg.getClass().getName());
                                retryFlag = true;
                                break;
                            } else {
                                newArgs[i] = newArg;
                            }
                        }
                    }
                    if (retryFlag) {
                        return invokeConsWithParams(className, descriptor, args, argsType, returnTypeName);
                    } else {
                        try {
                            Object result = matched.newInstance(newArgs);
                            Object currentVersion = getCurrentVersion(result);
                            if (currentVersion.getClass().getName().contains("_$fr$_")) {
                                if (isSameFrebelClass(result.getClass().getName(), returnTypeName)) {
                                    return getSpecificVersion(currentVersion, returnTypeName.replace("/", "."));
                                } else {
                                    String matchedClassName = frebelClass.getMatchedClassNameByParentClassName(returnTypeName);
                                    return getSpecificVersion(currentVersion, matchedClassName);
                                }
                            } else {
                                return result;
                            }
                        } catch (IllegalArgumentException e) {
                            // args type problem, retry
                            LOGGER.warn("Retry to find correct constructor because try to invoke constructor: {} failed", matched);
                            return invokeConsWithParams(className, descriptor, args, argsType, returnTypeName);
                        }
                    }
                } else {
                    String errorMsg = "no constructor found in Class:" + currentVersionClass +
                            ", constructor types are: " + Arrays.toString(argsType);
                    LOGGER.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }
            } else {
                return Class.forName(className).getConstructor(argsType).newInstance(args);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FrebelInvocationException(e);
        }
    }

    public static Object invokeWithNoParams(String methodName, Object invokeObj, String returnTypeName) {
        return invokeWithNoParams(methodName, invokeObj, Reflection.getCallerClass(2), returnTypeName);
    }

    public static Object invokeWithNoParams(String methodName, Object invokeObj, Class callerClass, String returnTypeName) {
        try {
            Method method;
            Object returnValue;
            Object currentVersion = getCurrentVersion(invokeObj);
            method = currentVersion.getClass().getMethod(methodName);
            try {
                returnValue = method.invoke(currentVersion);
                if (returnValue != null) {
                    FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                    if (returnTypeName.equals("void")) {
                        throw new IllegalStateException("expect null return value, but not null!");
                    } else if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                        return getSpecificVersion(returnValue, callerClass.getName());
                    } else if (frebelClass != null) {
                        String matchedName = frebelClass.getMatchedClassNameByParentClassName(returnTypeName);
                        return getSpecificVersion(returnValue, matchedName);
                    } else {
                        if (Class.forName(returnTypeName.replace("/", ".")).isInstance(returnValue)) {
                            return returnValue;
                        } else {
                            String errorMsg = "error return value: " + returnTypeName + "," +
                                    "real return value type is: " + returnValue.getClass();
                            LOGGER.error(errorMsg);
                            throw new FrebelInvocationException(errorMsg);
                        }
                    }
                } else {
                    return null;
                }
            } catch (IllegalArgumentException e) {
                // args type problem, retry
                LOGGER.warn("Retry to find correct method because try to invoke method: {} failed", method);
                return invokeWithNoParams(methodName, invokeObj, callerClass, returnTypeName);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FrebelInvocationException(e);
        }
    }


    private static boolean isMatchedMethod(Class<?>[] parameterTypes, Class<?>[] argsType) {
        if (parameterTypes.length != argsType.length) {
            return false;
        }
        boolean find = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Class<?> aClass = argsType[i];
            if (!parameterType.isAssignableFrom(aClass) && !isSameFrebelClass(aClass.getName(), parameterType.getName())) {
                // when this method's some one param is not subType of parameterType or not in the same FrebelClass,
                // we think this method doesn't match
                find = false;
                break;
            }
        }

        return find;
    }

    public static Object invokeWithParams(String methodName, Object invokeObj, Object[] args, Class<?>[] argsType, Class callerClass, String returnTypeCastTo) {
        try {
            // 1. find matched method
            Object currentVersion = getCurrentVersion(invokeObj);
            Method[] methods = currentVersion.getClass().getDeclaredMethods();
            Method findMethod = null;
            for (Method method : methods) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean find = isMatchedMethod(parameterTypes, argsType);
                if (find) {
                    findMethod = method;
                    break;
                }
            }

            // 2. assemble args
            Object[] newArgs = new Object[args.length];
            boolean retryFlag = false;
            if (findMethod != null) {
                Class<?>[] parameterTypes = findMethod.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].isAssignableFrom(argsType[i])) {
                        newArgs[i] = args[i];
                    } else if (isSameFrebelClass(parameterTypes[i].getName(), argsType[i].getName())) {
                        newArgs[i] = args[i] == null ? null : getSpecificVersion(getCurrentVersion(args[i]), parameterTypes[i].getName());
                    } else {
                        // retry because between 1 and 2 args class has benn reloaded,
                        // so the class version has been outdated
                        LOGGER.warn("Retry to find matched method, method: {}, args index: {}, method defined type: {}" +
                                        ", current version type: {}.",
                                findMethod,
                                i,
                                parameterTypes[i].getName(),
                                newArgs[i].getClass().getName());
                        retryFlag = true;
                        break;
                    }
                }
                if (retryFlag) {
                    LOGGER.warn("retry methodName: {}, invokeObjClass: {}, argsType: {}.", methodName,
                            invokeObj.getClass().getName(), Arrays.toString(argsType));
                    return invokeWithParams(methodName, invokeObj, args, argsType, callerClass, returnTypeCastTo);
                } else {
                    try {
                        findMethod.setAccessible(true);
                        Object returnValue = findMethod.invoke(currentVersion, newArgs);
                        if (returnValue != null) {
                            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                            if (returnTypeCastTo.equals("void")) {
                                throw new FrebelInvocationException("expect null return value, but not null!");
                            } else if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                                return getSpecificVersion(returnValue, callerClass.getName());
                            } else if (frebelClass != null) {
                                String matchedName = frebelClass.getMatchedClassNameByParentClassName(returnTypeCastTo);
                                return getSpecificVersion(returnValue, matchedName);
                            } else {
                                if (Class.forName(returnTypeCastTo.replace("/", ".")).isInstance(returnValue)) {
                                    return returnValue;
                                } else {
                                    String errorMsg = "error return value: " + returnTypeCastTo + "," +
                                            "real return value type is: " + returnValue.getClass() + ".";
                                    LOGGER.error(errorMsg);
                                    throw new FrebelInvocationException(errorMsg);
                                }
                            }
                        } else {
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        // args type problem, retry
                        LOGGER.warn("Retry to find correct method because try to invoke method: {} failed", findMethod);
                        return invokeWithParams(methodName, invokeObj, args, argsType, callerClass, returnTypeCastTo);
                    }
                }
            } else {
                // no matched method found
                String errorMsg = "No method found in Class:" + currentVersion.getClass().getName() +
                        ", method arguments types are: " + Arrays.toString(argsType);
                throw new FrebelInvocationException("no method found!");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FrebelInvocationException(e);
        }
    }

    public static Object invokeStaticWithParams(String methodName, Class clazz, Object[] args, Class<?>[] argsType, Class callerClass, String returnTypeCastTo) {
        try {
            // 1. find matched method
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(clazz.getName());
            Class finalClazz = clazz;
            if (frebelClass != null) {
                finalClazz = frebelClass.getCurrentVersionClass();
            }
            Method[] methods = finalClazz.getDeclaredMethods();
            Method findMethod = null;
            for (Method method : methods) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                boolean find = isMatchedMethod(parameterTypes, argsType);
                if (find) {
                    findMethod = method;
                    break;
                }
            }

            // 2. assemble args
            Object[] newArgs = new Object[args.length];
            boolean retryFlag = false;
            if (findMethod != null) {
                Class<?>[] parameterTypes = findMethod.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].isPrimitive()) {
                        newArgs[i] = args[i];
                    } else if (parameterTypes[i].isAssignableFrom(argsType[i])) {
                        newArgs[i] = args[i];
                    } else if (isSameFrebelClass(parameterTypes[i].getName(), argsType[i].getName())) {
                        newArgs[i] = args[i] == null ? null : getSpecificVersion(getCurrentVersion(args[i]), parameterTypes[i].getName());
                    } else {
                        // retry because between 1 and 2 args class has benn reloaded,
                        // so the class version has been outdated
                        LOGGER.warn("Retry to find matched method, method: {}, args index: {}, method defined type: {}" +
                                        ", current version type: {}.",
                                findMethod,
                                i,
                                parameterTypes[i].getName(),
                                newArgs[i].getClass().getName());
                        retryFlag = true;
                        break;
                    }
                }
                if (retryFlag) {
                    LOGGER.warn("retry methodName: {}, methodClass: {}, argsType: {}.", methodName,
                            finalClazz.getName(), Arrays.toString(argsType));
                    return invokeStaticWithParams(methodName, clazz, args, argsType, callerClass, returnTypeCastTo);
                } else {
                    try {
                        findMethod.setAccessible(true);
                        Object returnValue = findMethod.invoke(null, newArgs);
                        if (returnValue != null) {
                            frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                            if (returnTypeCastTo.equals("void")) {
                                throw new FrebelInvocationException("expect null return value, but not null!");
                            } else if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                                return getSpecificVersion(returnValue, callerClass.getName());
                            } else if (frebelClass != null) {
                                String matchedName = frebelClass.getMatchedClassNameByParentClassName(returnTypeCastTo);
                                return getSpecificVersion(returnValue, matchedName);
                            } else {
                                if (Class.forName(returnTypeCastTo.replace("/", ".")).isInstance(returnValue)) {
                                    return returnValue;
                                } else {
                                    String errorMsg = "error return value: " + returnTypeCastTo + "," +
                                            "real return value type is: " + returnValue.getClass() + ".";
                                    LOGGER.error(errorMsg);
                                    throw new FrebelInvocationException(errorMsg);
                                }
                            }
                        } else {
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        // args type problem, retry
                        LOGGER.warn("Retry to find correct method because try to invoke method: {} failed", findMethod);
                        return invokeStaticWithParams(methodName, clazz, args, argsType, callerClass, returnTypeCastTo);
                    }
                }
            } else {
                // no matched method found
                String errorMsg = "No method found in Class:" + clazz.getName() +
                        ", method arguments types are: " + Arrays.toString(argsType);
                throw new FrebelInvocationException("no method found!");
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FrebelInvocationException(e);
        }
    }

    public static Object invokeCast(Object toCast, String className) {
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(toCast.getClass().getName());
        if (frebelClass != null) {
            try {
                String matched = frebelClass.getMatchedClassNameByParentClassName(className);
                toCast = getSpecificVersion(toCast, matched);
            } catch (IllegalStateException e) {
                LOGGER.warn("FrebelClass: {} doesn't has subclass of type: {}",
                        frebelClass.getOriginName(),
                        className);
                throw new ClassCastException("Cannot cast " + frebelClass.getOriginName() + " to " + className);
            }
        }
        try {
            Class<?> cls = Class.forName(className);
            return cls.cast(toCast);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static int invokeInstanceOf(Object toCast, String className) {
        if (toCast == null) {
            return 0;
        }
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(toCast.getClass().getName());
        if (frebelClass != null) {
            try {
                String matched = frebelClass.getMatchedClassNameByParentClassName(className);
                toCast = getSpecificVersion(toCast, matched);
            } catch (IllegalStateException e) {
                LOGGER.warn("FrebelClass: {} doesn't has subclass of type: {}",
                        frebelClass.getOriginName(),
                        className);
                return 0;
            }
        }
        try {
            Class<?> cls = Class.forName(className);
            cls.cast(toCast);
            return 1;
        } catch (ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return 0;
        } catch (ClassCastException e) {
            return 0;
        }
    }

    /*********** instance redirect methods *************/
    public static Object invokeInstanceMethodsWithWrapperParams(Object target, Object[] wrapperParams, String methodName, String descriptor, String returnTypeCastTo) {
        Object[] newParams = new Object[wrapperParams.length];
        for (int i = 0; i < wrapperParams.length; i++) {
            if (wrapperParams[i] instanceof PrimitiveWrapper) {
                wrapperParams[i] = ((PrimitiveWrapper) wrapperParams[i]).unwrap();
            }
        }
        return invokeWithParams(methodName, target, wrapperParams, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }
    /*********** static redirect methods *************/
    public static Object invokeStaticMethodsWithWrapperParams(Object[] wrapperParams, String methodName, String descriptor, String returnTypeCastTo, String ownerClassName) {
        Object[] newParams = new Object[wrapperParams.length];
        for (int i = 0; i < wrapperParams.length; i++) {
            if (wrapperParams[i] instanceof PrimitiveWrapper) {
                wrapperParams[i] = ((PrimitiveWrapper) wrapperParams[i]).unwrap();
            }
        }
        try {
            return invokeStaticWithParams(methodName, Class.forName(ownerClassName), wrapperParams, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static Object invokeWithParams(String methodName, Object invokeObj, Object[] args, Class<?>[] argsType, String returnTypeName) {
        return invokeWithParams(methodName, invokeObj, args, argsType, Reflection.getCallerClass(2), returnTypeName);
    }

    public static Object invokeWith0Params(Object target, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithNoParams(methodName, target, Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith1Params(Object target, Object arg1, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith2Params(Object target, Object arg1, Object arg2, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith3Params(Object target, Object arg1, Object arg2, Object arg3, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith4Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith5Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith6Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,Object arg6, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5,arg6}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith7Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,Object arg6,Object arg7, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5,arg6,arg7}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith8Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,Object arg6,Object arg7, Object arg8, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5,arg6,arg7,arg8}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith9Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,Object arg6,Object arg7, Object arg8,Object arg9, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5,arg6,arg7,arg8,arg9}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    public static Object invokeWith10Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10, String methodName, String descriptor, String returnTypeCastTo) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5,arg6,arg7,arg8,arg9,arg10}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2), returnTypeCastTo);
    }

    /*********** constructor redirect methods *************/
    public static Object invokeConsWithWrapperParams(Object[] wrapperParams, String className, String descriptor, String returnTypeCastTo) {
        Object[] newParams = new Object[wrapperParams.length];
        for (int i = 0; i < wrapperParams.length; i++) {
            if (wrapperParams[i] instanceof PrimitiveWrapper) {
                wrapperParams[i] = ((PrimitiveWrapper) wrapperParams[i]).unwrap();
            }
        }
        return invokeConsWithParams(className, descriptor, wrapperParams, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static void main(String[] args) throws NotFoundException, CannotCompileException {
        ClassPool aDefault = ClassPool.getDefault();
    }

    public static Object invokeConsWith0Params(String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithNoParams(className, descriptor, returnTypeCastTo);
    }

    public static Object invokeConsWith1Params(Object arg1, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith2Params(Object arg1,Object arg2, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith3Params(Object arg1,Object arg2,Object arg3 ,String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith4Params(Object arg1,Object arg2,Object arg3,Object arg4,String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith5Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith6Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith7Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith8Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith9Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith10Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith11Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith12Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11,Object arg12, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }
    public static Object invokeConsWith13Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11,Object arg12,Object arg13, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith14Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11,Object arg12,Object arg13,Object arg14, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13,arg14}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeConsWith15Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11,Object arg12,Object arg13,Object arg14,Object arg15, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13,arg14,arg15}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }
    public static Object invokeConsWith16Params(Object arg1,Object arg2,Object arg3,Object arg4,Object arg5,Object arg6,Object arg7, Object arg8,Object arg9,Object arg10,Object arg11,Object arg12,Object arg13,Object arg14,Object arg15,Object arg16, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1,arg2,arg3,arg4,arg5,arg6,arg7,arg8,arg9,arg10,arg11,arg12,arg13,arg14,arg15,arg16}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
    }

    public static Object invokeGetField(Object target, String owner, String fieldName, String className) {
        return invokeWithNoParams("_$fr$_$g$" + fieldName, target, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, Object arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{arg.getClass()}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, int arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Integer.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, byte arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Byte.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, short arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Short.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, boolean arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Boolean.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, long arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Long.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, double arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Double.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, float arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Float.class}, Reflection.getCallerClass(2), className);
    }

    public static void invokeSetField(Object target, char arg, String owner, String fieldName, String className) {
        invokeWithParams("_$fr$_$s$" + fieldName, target, new Object[]{arg}, new Class[]{Float.class}, Reflection.getCallerClass(2), className);
    }

    public static String getMethodName(int paramsNum) {
        return "invokeWith" + paramsNum + "Params";
    }

    public static String getDesc(int paramsNum) {
        String frebelMethodName = getMethodName(paramsNum);
        try {
            CtMethod[] methods = ClassPool.getDefault().get(FrebelRuntime.class.getName()).getMethods();
            for (CtMethod method : methods) {
                if (method.getName().equals(frebelMethodName)) {
                    return method.getSignature();
                }
            }
            LOGGER.error("Failed to find {} params invoke method.", paramsNum);
            throw new IllegalArgumentException();
        } catch (NotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new IllegalArgumentException(e);
        }
    }

    public static String getConsMethodName(int paramsNum) {
        return "invokeConsWith" + paramsNum + "Params";
    }

    public static Method getConsMethodInstance(int paramsNum) {
        return Arrays.stream(FrebelRuntime.class.getMethods())
                .filter(i -> i.getName().equals(getConsMethodName(paramsNum))).findFirst().get();
    }

    public static String getConsDesc(int paramsNum) {
        String frebelMethodName = getConsMethodName(paramsNum);
        try {
            CtMethod[] methods = ClassPool.getDefault().get(FrebelRuntime.class.getName()).getMethods();
            for (CtMethod method : methods) {
                if (method.getName().equals(frebelMethodName)) {
                    return method.getSignature();
                }
            }
            throw new IllegalArgumentException();
        } catch (NotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static Class<?>[] getClassArrayFromDesc(String desc) {
        try {
            CtClass[] ctClasses = Descriptor.getParameterTypes(desc, ClassPool.getDefault());
            if (ctClasses == null || ctClasses.length == 0) {
                throw new IllegalStateException();
            }
            Class<?>[] classes = new Class[ctClasses.length];
            for (int i = 0; i < ctClasses.length; i++) {
                if (ctClasses[i].isPrimitive()) {
                    classes[i] = PrimitiveTypeUtil.getPrimitiveClass(ctClasses[i].getName());
                } else {
                    classes[i] = Class.forName(ctClasses[i].getName());
                }
            }
            return classes;
        } catch (NotFoundException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static String invokeGetFieldDesc() {
        return "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;";
    }

    public static String invokeSetFieldDesc(String desc) {
        String className = Descriptor.toClassName(desc);
        if (PrimitiveTypeUtil.isPrimitive(className)) {
            return "(Ljava/lang/Object;" + desc + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
        } else {
            return "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
        }
    }

    public static String invokeCastDesc() {
        return "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
    }

    public static String invokeInstanceOfDesc() {
        return "(Ljava/lang/Object;Ljava/lang/String;)I";
    }
}
