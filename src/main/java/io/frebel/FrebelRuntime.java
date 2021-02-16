package io.frebel;

import io.frebel.util.Descriptor;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import sun.reflect.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static io.frebel.FrebelClassRegistry.isSameFrebelClass;

public class FrebelRuntime {
    public static Object getCurrentVersion(Object obj) {
        try {
            Method method;
            try {
                method = obj.getClass().getMethod("_$fr$_getUid");
            } catch (NoSuchMethodException e) {
                return obj;
            }
            String uid = (String) method.invoke(obj);
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(obj.getClass().getName());
            Object currentVersion = FrebelObjectManager.getSpecificVersionObject(uid, frebelClass.getCurrentVersionClassName());
            if (currentVersion == null) {
                currentVersion = obj;
            }
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

    public static Object getSpecificVersion(Object obj, String className) {
        Method method;
        try {
            method = obj.getClass().getMethod("_$fr$_getUid");
            String uid = (String) method.invoke(obj);
            Object result = FrebelObjectManager.getSpecificVersionObject(uid, className);
            if (result == null) {
                return FrebelObjectManager.createObjectOf(obj, className);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return obj;
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

    public static Object invokeConsWithNoParams(String className, String descriptor) {
        className = className.replace("/", ".");
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        try {
            if (frebelClass != null) {
                // reloaded
                Object result = Class.forName(className).newInstance();
                getCurrentVersion(result);
                return result;
            } else {
                return Class.forName(className).newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Object invokeConsWithParams(String className, String descriptor, Object[] args, Class[] argsType) {
        className = className.replace("/", ".");
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        try {
            if (frebelClass != null) {
                Class<?> currentVersionClass = Class.forName(frebelClass.getCurrentVersionClassName());
                Constructor<?>[] constructors = currentVersionClass.getConstructors();
                Constructor matched = null;
                for (Constructor<?> constructor : constructors) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (isMatchedMethod(parameterTypes, argsType)) {
                        matched = constructor;
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
                                retryFlag = true;
                            } else {
                                newArgs[i] = newArg;
                            }
                        }
                    }
                    if (retryFlag) {
                        return invokeConsWithParams(className, descriptor, args, argsType);
                    } else {
                        try {
                            Object returnValue = matched.newInstance(newArgs);
                            return getSpecificVersion(returnValue, className);
                        } catch (IllegalArgumentException e) {
                            // args type problem, retry
                            return invokeConsWithParams(className, descriptor, args, argsType);
                        }
                    }
                } else {
                    throw new IllegalStateException("no constructor found in Class:" + currentVersionClass +
                            ", constructor types are: " + Arrays.toString(argsType));
                }
            } else {
                return Class.forName(className).getConstructor(argsType).newInstance(args);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Object invokeWithNoParams(String methodName, Object invokeObj) {
        return invokeWithNoParams(methodName, invokeObj, Reflection.getCallerClass(2));
    }

    public static Object invokeWithNoParams(String methodName, Object invokeObj, Class callerClass) {
        try {
            try {
                Object returnValue;
                Object currentVersion = getCurrentVersion(invokeObj);
                Method method = currentVersion.getClass().getMethod(methodName);
                returnValue = method.invoke(currentVersion);

                if (returnValue != null) {
                    if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                        return getSpecificVersion(returnValue, callerClass.getName());
                    } else if (returnValue.getClass().getName().contains("_$fr$")){
                        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                        String originName = frebelClass.getOriginName();
                        return getSpecificVersion(returnValue, originName);
                    } else {
                        return returnValue;
                    }
                } else {
                    return returnValue;
                }
            } catch (IllegalArgumentException e) {
                // args type problem, retry
                return invokeWithNoParams(methodName, invokeObj, callerClass);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeWithParams(String methodName, Object invokeObj, Object[] args, Class<?>[] argsType) {
        return invokeWithParams(methodName, invokeObj, args, argsType, Reflection.getCallerClass(2));
    }


    private static boolean isMatchedMethod(Class[] parameterTypes, Class[] argsType) {
        if (parameterTypes.length != argsType.length) {
            return false;
        }
        boolean find = true;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Class<?> aClass = argsType[i];
            if (parameterType.getName().equals(aClass.getName())) {
                continue;
            } else if (isSameFrebelClass(aClass.getName(), parameterType.getName())) {
                continue;
            } else {
                find = false;
                break;
            }
        }

        return find;
    }

    public static Object invokeWithParams(String methodName, Object invokeObj, Object[] args, Class<?>[] argsType, Class callerClass) {
        try {
            // 1. find matched method
            Object currentVersion = getCurrentVersion(invokeObj);
            Method[] methods = currentVersion.getClass().getMethods();
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
                    if (parameterTypes[i].getName().equals(argsType[i].getName())) {
                        newArgs[i] = args[i];
                    } else if (isSameFrebelClass(parameterTypes[i].getName(), argsType[i].getName())) {
                        Object newArg = getCurrentVersion(args[i]);
                        if (newArg.getClass() != parameterTypes[i]) {
                            // retry because between 1 and 2 args class has benn reloaded,
                            // so the class version has been outdated
                            retryFlag = true;
                        } else {
                            newArgs[i] = newArg;
                        }
                    }
                }
                if (retryFlag) {
                    return invokeWithParams(methodName, invokeObj, args, argsType, callerClass);
                } else {
                    try {
                        Object returnValue = findMethod.invoke(currentVersion, newArgs);
                        if (returnValue != null) {
                            if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                                return getSpecificVersion(returnValue, callerClass.getName());
                            } else if (returnValue.getClass().getName().contains("_$fr$")){
                                FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                                String originName = frebelClass.getOriginName();
                                return getSpecificVersion(returnValue, originName);
                            } else {
                                return returnValue;
                            }
                        } else {
                            return returnValue;
                        }
                    } catch (IllegalArgumentException e) {
                        // args type problem, retry
                        return invokeWithParams(methodName, invokeObj, args, argsType, callerClass);
                    }
                }
            } else {
                // no matched method found
                throw new IllegalStateException("no method found!");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeWith0Params(Object target, String methodName, String descriptor) {
        return invokeWithNoParams(methodName, target, Reflection.getCallerClass(2));
    }

    public static Object invokeWith1Params(Object target, Object arg1, String methodName, String descriptor) {
        return invokeWithParams(methodName, target, new Object[]{arg1}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2));
    }

    public static Object invokeWith2Params(Object target, Object arg1, Object arg2, String methodName, String descriptor) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2));
    }

    public static Object invokeWith3Params(Object target, Object arg1, Object arg2, Object arg3, String methodName, String descriptor) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2));
    }

    public static Object invokeWith4Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, String methodName, String descriptor) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2));
    }

    public static Object invokeWith5Params(Object target, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, String methodName, String descriptor) {
        return invokeWithParams(methodName, target, new Object[]{arg1, arg2, arg3, arg4, arg5}, getClassArrayFromDesc(descriptor), Reflection.getCallerClass(2));
    }
    /*********** constructor redirect methods *************/
    public static Object invokeConsWith0Params(String className, String descriptor) {
        return invokeConsWithNoParams(className, descriptor);
    }

    public static Object invokeConsWith1Params(Object arg1, String className, String descriptor) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1}, getClassArrayFromDesc(descriptor));
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
            throw new IllegalArgumentException();
        } catch (NotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public static String getConsMethodName(int paramsNum) {
        return "invokeConsWith" + paramsNum + "Params";
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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static Class[] getClassArrayFromDesc(String desc) {
        try {
            CtClass[] ctClasses = Descriptor.getParameterTypes(desc, ClassPool.getDefault());
            if (ctClasses == null && ctClasses.length == 0) {
                throw new IllegalStateException();
            }
            Class[] classes = new Class[ctClasses.length];
            for (int i = 0; i < ctClasses.length; i++) {
                classes[i] = Class.forName(ctClasses[i].getName());
            }
            return classes;
        } catch (NotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
