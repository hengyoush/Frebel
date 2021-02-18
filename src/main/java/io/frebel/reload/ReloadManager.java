package io.frebel.reload;

import io.frebel.ClassInner;
import io.frebel.FrebelClass;
import io.frebel.FrebelClassRegistry;
import io.frebel.FrebelJVM;
import io.frebel.FrebelProps;
import io.frebel.bcp.AddFieldAccessorBCP;
import io.frebel.bcp.AddForwardBCP;
import io.frebel.bcp.AddUidBCP;
import io.frebel.bcp.ByteCodeProcessor;
import io.frebel.bcp.FieldRedirectBCP;
import io.frebel.bcp.MethodRedirectBCP;
import io.frebel.bcp.RenameBCP;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum ReloadManager {
    INSTANCE;
    private ByteCodeProcessor renameBCP = new RenameBCP();
    private ByteCodeProcessor addForward = new AddForwardBCP();
    private ByteCodeProcessor uidBCP = new AddUidBCP();
    private ByteCodeProcessor redirectBCP = new MethodRedirectBCP();
    private ByteCodeProcessor addFieldAccessorBCP = new AddFieldAccessorBCP();
    private ByteCodeProcessor fieldRedirectBCP = new FieldRedirectBCP();

    public ClassInner reloadForAddForwardDelayed(ClassInner classInner) {
        try {
            String className = classInner.getOriginClassName();
            byte[] bytes = classInner.getBytes();
            byte[] processed = addForward.process(ClassPool.getDefault().getClassLoader(), bytes);
            FrebelJVM.getInstrumentation().redefineClasses(new ClassDefinition(Class.forName(className), processed));
            if (FrebelProps.debugClassFile()) {
                CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(processed), false);
                ctClass.debugWriteFile("./");
            }
            return new ClassInner(processed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void reload(ClassInner classInner/* 新版本的class文件 */) throws Exception {
        reload(classInner, true, false);
    }

    public ClassInner reload(ClassInner classInner, boolean updateClassVersion, boolean addForwardDelayed) throws Exception {
        String className = classInner.getOriginClassName();
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);

        boolean redefineFlag = false;
        byte[] processed = null;
        if (frebelClass == null) {
            // 第一次修改
            ClassPool classPool = ClassPool.getDefault();
            CtClass ctClass = classPool.get(className);
            byte[] bytes = ctClass.toBytecode();

            processed = addForward.process(classPool.getClassLoader(), bytes);
            if (FrebelProps.debugClassFile()) {
                classPool.makeClass(new ByteArrayInputStream(processed), false).debugWriteFile("./");
            }

            frebelClass = new FrebelClass(processed, classPool.getClassLoader());
            FrebelClassRegistry.register(className, frebelClass);
            redefineFlag = true;
        }
        // modify
        byte[] bytes = classInner.getBytes();
        bytes = renameBCP.process(frebelClass.getClassLoader(), bytes);
        bytes = addFieldAccessorBCP.process(frebelClass.getClassLoader(), bytes);
        if (!addForwardDelayed) {
            bytes = addForward.process(frebelClass.getClassLoader(), bytes);
        }
        bytes = fieldRedirectBCP.process(frebelClass.getClassLoader(), bytes);
        bytes = redirectBCP.process(frebelClass.getClassLoader(), bytes);
        bytes = uidBCP.process(frebelClass.getClassLoader(), bytes);
        ClassInner newClassInner = new ClassInner(bytes);
        // load new class
        CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytes), false);
        if (FrebelProps.debugClassFile()) {
            ctClass.debugWriteFile("./");
        }
        ctClass.toClass();
        // redefine old class, add Forward
        if (redefineFlag) {
            try {
                FrebelJVM.getInstrumentation().redefineClasses(new ClassDefinition(Class.forName(className), processed));
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("class redefine error, class: " + className);
                throw new RuntimeException(e);
            }
        }

        if (updateClassVersion) {
            frebelClass.addNewVersionClass(newClassInner);
        }

        return newClassInner;
    }

    public void loadNewClass(ClassInner... classInner) throws Exception {
        for (ClassInner inner : classInner) {
            try {
                Class.forName(inner.getOriginClassName());
                continue;
            } catch (Exception e) {
                // ignore
            }
            ClassPool.getDefault().makeClass(new ByteArrayInputStream(inner.getBytes()), false).toClass();
        }
    }

    public void batchReload(ClassInner... classInners) throws Exception {
        if (classInners == null) {
            return;
        }

        if (classInners.length == 1) {
            reload(classInners[0]);
            return;
        }

        List<ClassInner> finaClassInner = new ArrayList<>(classInners.length);
        for (ClassInner classInner : classInners) {
            finaClassInner.add(reload(classInner, false, false));
        }

        for (ClassInner classInner : finaClassInner) {
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(classInner.getOriginClassName());
            frebelClass.addNewVersionClass(classInner);
        }
    }

    public void batchReload2(ClassInner... classInners) throws Exception {
        if (classInners == null) {
            return;
        }
        // new method
        if (classInners.length == 1) {
            reload(classInners[0]);
            return;
        }

        Map<String, ClassInner> classInnerMap = new HashMap<>();
        for (ClassInner classInner : classInners) {
            classInnerMap.put(classInner.getOriginClassName(), classInner);
        }

        Set<MethodInfo> newMethods = new HashSet<>();
        for (ClassInner classInner : classInners) {
            String originClassName = classInner.getOriginClassName();
            Set<MethodInfo> oldClassMethods = getOldClassMethods(originClassName);
            Set<MethodInfo> newClassMethods = getNewClassMethods(classInnerMap, originClassName);
            newClassMethods.removeAll(oldClassMethods);
            newMethods.addAll(newClassMethods);
        }

        // search all references to old class，replace the class name to new one
//        for (ClassInner classInner : classInners) {
//            List<MethodInfo> constantPoolMethods = classInner.getConstantPoolMethods();
//            for (MethodInfo newMethod : newMethods) {
//                if (!constantPoolMethods.contains(newMethod)) {
//                    continue;
//                }
//
//                String newClassName = getNextVersionClassName(newMethod.getClassName());
//                // 修改使用到新方法的类名
//                classInner.updateConstantMethodClassName(newMethod.getClassName(), newClassName);
//                // 修改class中field类名
//                classInner.updateConstantFieldClassName(newMethod.getClassName(), newClassName);
//            }
//        }

        // create new class if any class inherited modified classes
        List<ClassInner> finalReloadClass = Arrays.asList(classInners);
        Class[] allLoadedClasses = FrebelJVM.getInstrumentation().getAllLoadedClasses();
        Map<String, Set<String>> classInheritMap = new HashMap<>(256);
        Map<String, Set<String>> interfaceInheritMap = new HashMap<>(256);
        for (Class c : allLoadedClasses) {
            if (c.getClassLoader() != null &&
                    c.getSuperclass() != Object.class &&
                    c.getSuperclass() != null) {
                Set<String> set = classInheritMap.get(c.getSuperclass().getName());
                if (set == null) {
                    classInheritMap.put(c.getSuperclass().getName(), new HashSet<String>(4));
                    set = classInheritMap.get(c.getSuperclass().getName());
                }
                set.add(c.getName());
            }

            if (c.getClassLoader() != null &&
                    c.getSuperclass() != Object.class &&
                    c.getSuperclass() != null) {
                Class[] interfaces = c.getInterfaces();
                for (Class anInterface : interfaces) {
                    Set<String> set = interfaceInheritMap.get(anInterface.getName());
                    if (set == null) {
                        interfaceInheritMap.put(c.getSuperclass().getName(), new HashSet<String>(4));
                        set = interfaceInheritMap.get(c.getSuperclass().getName());
                    }
                    set.add(c.getName());
                }
            }
        }

        Set<String> alreadyHandled = new HashSet<>(classInnerMap.keySet());
        List<String> forSearch = new ArrayList<>(classInnerMap.keySet());
        int i = 0;
        while (i < forSearch.size()) {
            String cls = forSearch.get(i);
            Set<String> subClasses = classInheritMap.get(cls);
            if (subClasses != null) {
                for (String subClass : subClasses) {
                    if (!alreadyHandled.contains(subClass)) {
                        forSearch.add(subClass);
                        ClassInner classInner = new ClassInner(ClassPool.getDefault().get(subClass).toBytecode());
                        finalReloadClass.add(classInner);
                    }
                }
            }

            Set<String> subInterfaces = interfaceInheritMap.get(cls);
            if (subInterfaces != null) {
                for (String subInterface : subInterfaces) {
                    if (!alreadyHandled.contains(subInterface)) {
                        forSearch.add(subInterface);
                        ClassInner classInner = new ClassInner(ClassPool.getDefault().get(subInterface).toBytecode());
                        finalReloadClass.add(classInner);
                    }
                }
            }
            i++;
        }

        // modify the super class name and interface name if necessary
        Map<String, ClassInner> finalClassInnerMap = new HashMap<>(finalReloadClass.size());
        for (ClassInner reloadClass : finalReloadClass) {
            finalClassInnerMap.put(reloadClass.getOriginClassName(), reloadClass);
        }

        for (ClassInner reloadClass : finalReloadClass) {
            String superClassName = reloadClass.getSuperClassName();
            List<String> interfaces = reloadClass.getInterfaces();
            if (finalClassInnerMap.containsKey(superClassName)) {
                reloadClass.updateSuperClassName(getNextVersionClassName(superClassName));
            }
            for (String anInterface : interfaces) {
                if (finalClassInnerMap.containsKey(anInterface)) {
                    reloadClass.updateInterfaceName(anInterface, getNextVersionClassName(anInterface));
                }
            }

            for (ClassInner inner : finalReloadClass) {
                if (inner.getOriginClassName().equals(reloadClass.getOriginClassName())) {
                    continue;
                }
                reloadClass.updateNameTypeAndClassInfo(inner.getOriginClassName(),
                        getNextVersionClassName(inner.getOriginClassName()));
            }
        }

        // reload
        Map<String, ClassInner> finalClasses = new HashMap<>();
        for (ClassInner modifyClass : finalReloadClass) {
            finalClasses.put(modifyClass.getOriginClassName(), reload(modifyClass, false, true));
        }

        Map<String, ClassInner> finalClasses2 = new HashMap<>();
        for (String c : finalClasses.keySet()) {
            ClassInner classInner = finalClasses.get(c);
            classInner = reloadForAddForwardDelayed(classInner);
            finalClasses2.put(c, classInner);
        }

        for (String className : finalClasses2.keySet()) {
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
            frebelClass.addNewVersionClass(finalClasses2.get(className));
        }
    }

    private String getNextVersionClassName(String className) {
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        String newClassName;
        if (frebelClass == null) {
            // class contains the new method was modified first time
            newClassName = className + "_$fr$_" + 1;
        } else {
            newClassName = frebelClass.getNextVersionClassName();
        }

        return newClassName;
    }

    private Set<MethodInfo> getOldClassMethods(String className) {
        try {
            Set<MethodInfo> result = new HashSet<>();
            CtClass ctClass = ClassPool.getDefault().get(className);
            CtMethod[] methods = ctClass.getMethods();
            for (CtMethod ctMethod : methods) {
                result.add(new MethodInfo(className, ctMethod.getName(), ctMethod.getSignature()));
            }
            CtConstructor[] constructors = ctClass.getConstructors();
            for (CtConstructor constructor : constructors) {
                result.add(new MethodInfo(className, "<init>", constructor.getSignature()));
            }
            return result;
        } catch (NotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Set<MethodInfo> getNewClassMethods(Map<String, ClassInner> modifiedClassMap, String className) {
        if (className == null) {
            return new HashSet<>(0);
        }
        if (modifiedClassMap.containsKey(className)) {
            ClassInner classInner = modifiedClassMap.get(className);
            Set<MethodInfo> declaredMethods = new HashSet<>(classInner.getDeclaredMethods(true, true));
            String superClassName = classInner.getSuperClassName();
            Set<MethodInfo> superDecMethods = getNewClassMethods(modifiedClassMap, superClassName);
            List<String> interfaces = classInner.getInterfaces();
            for (String i : interfaces) {
                Set<MethodInfo> interfaceDecMethods = getNewClassMethods(modifiedClassMap, i);
                declaredMethods.addAll(interfaceDecMethods);
            }
            declaredMethods.addAll(superDecMethods);
            return declaredMethods;
        } else {
            try {
                Set<MethodInfo> result = new HashSet<>();
                CtClass ctClass = ClassPool.getDefault().get(className);
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                CtConstructor[] declaredConstructors = ctClass.getDeclaredConstructors();
                for (CtMethod method : declaredMethods) {
                    if (!Modifier.isPrivate(method.getModifiers())) {
                        result.add(new MethodInfo(className, method.getName(), method.getSignature()));
                    }
                }
                for (CtConstructor constructor : declaredConstructors) {
                    if (!Modifier.isPrivate(constructor.getModifiers())) {
                        result.add(new MethodInfo(className, "<init>", constructor.getSignature()));
                    }
                }
                if (ctClass.getSuperclass() != null) {
                    Set<MethodInfo> superDecMethods = getNewClassMethods(modifiedClassMap, ctClass.getSuperclass().getName());
                    result.addAll(superDecMethods);
                }
                CtClass[] interfaces = ctClass.getInterfaces();
                for (CtClass anInterface : interfaces) {
                    Set<MethodInfo> interfaceDecMethods = getNewClassMethods(modifiedClassMap, anInterface.getName());
                    result.addAll(interfaceDecMethods);
                }
                return result;
            } catch (NotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * TODO 1 接口新增方法支持 2 继承方法支持
     * @param classInners
     * @throws Exception
     */
//    public void batchReload(ClassInner... classInners) throws Exception {
//        if (classInners == null) {
//            return;
//        }
//        // new method
//        if (classInners.length == 1) {
//            reload(classInners[0]);
//            return;
//        }
//
//        List<ClassInner> modifyClasses = new ArrayList<>();
//        List<MethodInfo> newMethods = new ArrayList<>();
//        // extract new method from class file
//        for (ClassInner classInner : classInners) {
//            String originClassName = classInner.getOriginClassName();
//            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(originClassName);
//            List<MethodInfo> oldMethods;
//            if (frebelClass == null) {
//                // 第一次修改
//                CtClass ctClass = ClassPool.getDefault().get(classInner.getOriginClassName());
//                CtMethod[] ctMethods = ctClass.getDeclaredMethods(); // TODO 是否获取全部的方法？
//                CtConstructor[] constructors = ctClass.getDeclaredConstructors();
//                oldMethods = new ArrayList<>(ctMethods.length);
//                for (CtMethod ctMethod : ctMethods) {
//                    if (!ctMethod.getName().contains("_$fr$")) {
//                        oldMethods.add(new MethodInfo(classInner.getOriginClassName(), ctMethod.getName(), ctMethod.getSignature()));
//                    }
//                }
//                for (CtConstructor constructor : constructors) {
//                    oldMethods.add(new MethodInfo(classInner.getOriginClassName(), "<init>", constructor.getSignature()));
//                }
//            } else {
//                ClassInner oldClassInner = frebelClass.getCurrentVersionClass();
//                oldMethods = oldClassInner.getDeclaredMethods();
//            }
//            List<MethodInfo> _newMethods = new ArrayList<>(classInner.getDeclaredMethods());
//            _newMethods.removeAll(oldMethods);
//            newMethods.addAll(_newMethods);
//            modifyClasses.add(classInner);
//        }
//
//        // search all references to new methods，replace the class name
//        for (ClassInner classInner : classInners) {
//            String className = classInner.getOriginClassName();
//            List<MethodInfo> constantPoolMethods = classInner.getConstantPoolMethods();
//            for (MethodInfo newMethod : newMethods) {
//                if (Objects.equals(newMethod.getClassName(), className)) {
//                    continue;
//                }
//                if (!constantPoolMethods.contains(newMethod)) {
//                    continue;
//                }
//
//                FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(newMethod.getClassName());
//                String newClassName;
//                if (frebelClass == null) {
//                    // class contains the new method was modified first time
//                    newClassName = newMethod.getClassName() + "_$fr$_" + 1;
//                } else {
//                    newClassName = frebelClass.getNextVersionClassName();
//                }
//                // 修改使用到新方法的类名
//                boolean isUpdated = classInner.updateConstantMethodClassName(newMethod, newClassName);
//                // 修改class中field类名
//                if (isUpdated) {
//                    classInner.updateConstantFieldClassName(newMethod.getClassName(), newClassName);
//                }
//            }
//        }
//
//        // reload
//        Map<String, ClassInner> finalClasses = new HashMap<>(modifyClasses.size());
//        for (ClassInner modifyClass : modifyClasses) {
//            finalClasses.put(modifyClass.getOriginClassName(), reload(modifyClass, false));
//        }
//
//        for (String className : finalClasses.keySet()) {
//            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
//            frebelClass.addNewVersionClass(finalClasses.get(className));
//        }
//    }

//    boolean compareAndDecideNeedCreateNewOne(ClassInner old, ClassInner _new) {
//        String oldSuperClassName = old.getSuperClassName();
//        String newSuperClassName = _new.getSuperClassName();
//        boolean needCreate = false;
//        if (!Objects.equals(oldSuperClassName, newSuperClassName)) {
//            return true;
//        }
//
//        int oldConstantPoolCount = old.getConstantPoolCount();
//        int newConstantPoolCount = _new.getConstantPoolCount();
//        if (oldConstantPoolCount != newConstantPoolCount) {
//            return true;
//        }
//
//        // TODO 判断常量池item
//
//    }
}
