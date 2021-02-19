package io.frebel;

import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import sun.reflect.Reflection;

import java.lang.reflect.Constructor;
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
                            // 创建新对象 && 状态拷贝
                            Object newClassInstance = FrebelObjectManager.createUninitializedObject(Class.forName(currentVersionClassName));
                            FrebelObjectManager.copyState(currentVersion, newClassInstance);
                            // 注册新对象
                            FrebelObjectManager.register(uid, newClassInstance);
                            FrebelObjectManager.clearState(currentVersion);
                            return newClassInstance;
                        } else {
                            return currentVersion;
                        }
                    }
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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static Object invokeConsWithParams(String className, String descriptor, Object[] args, Class[] argsType, String returnTypeName) {
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
                            return invokeConsWithParams(className, descriptor, args, argsType, returnTypeName);
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

    public static Object invokeWithNoParams(String methodName, Object invokeObj, String returnTypeName) {
        return invokeWithNoParams(methodName, invokeObj, Reflection.getCallerClass(2), returnTypeName);
    }

    public static Object invokeWithNoParams(String methodName, Object invokeObj, Class callerClass, String returnTypeName) {
        try {
            try {
                Object returnValue;
                Object currentVersion = getCurrentVersion(invokeObj);
                Method method = currentVersion.getClass().getMethod(methodName);
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
                            throw new IllegalStateException("error return value: " + returnTypeName + "," +
                                    "real return value type is: " + returnValue.getClass());
                        }
                    }
                } else {
                    return returnValue;
                }
            } catch (IllegalArgumentException e) {
                // args type problem, retry
                return invokeWithNoParams(methodName, invokeObj, callerClass, returnTypeName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            } else if (parameterType.isAssignableFrom(aClass)) {
                continue;
            } else {
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
                        retryFlag = true;
                        break;
                    }
                }
                if (retryFlag) {
                    System.out.printf("retry methodName: %s, invokeObjClass: %s, argsType: %s\n", methodName,
                            invokeObj.getClass().getName(), Arrays.toString(argsType));
                    return invokeWithParams(methodName, invokeObj, args, argsType, callerClass, returnTypeCastTo);
                } else {
                    try {
                        findMethod.setAccessible(true);
                        Object returnValue = findMethod.invoke(currentVersion, newArgs);
                        if (returnValue != null) {
                            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(returnValue.getClass().getName());
                            if (returnTypeCastTo.equals("void")) {
                                throw new IllegalStateException("expect null return value, but not null!");
                            } else if (isSameFrebelClass(callerClass.getName(), returnValue.getClass().getName())) {
                                return getSpecificVersion(returnValue, callerClass.getName());
                            } else if (frebelClass != null) {
                                String matchedName = frebelClass.getMatchedClassNameByParentClassName(returnTypeCastTo);
                                return getSpecificVersion(returnValue, matchedName);
                            } else {
                                if (Class.forName(returnTypeCastTo.replace("/", ".")).isInstance(returnValue)) {
                                    return returnValue;
                                } else {
                                    throw new IllegalStateException("error return value: " + returnTypeCastTo + "," +
                                            "real return value type is: " + returnValue.getClass());
                                }
                            }
                        } else {
                            return returnValue;
                        }
                    } catch (IllegalArgumentException e) {
                        // args type problem, retry
                        return invokeWithParams(methodName, invokeObj, args, argsType, callerClass, returnTypeCastTo);
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

    /*********** constructor redirect methods *************/
    public static Object invokeConsWith0Params(String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithNoParams(className, descriptor, returnTypeCastTo);
    }

    public static Object invokeConsWith1Params(Object arg1, String className, String descriptor, String returnTypeCastTo) {
        return invokeConsWithParams(className, descriptor, new Object[]{arg1}, getClassArrayFromDesc(descriptor), returnTypeCastTo);
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
}
