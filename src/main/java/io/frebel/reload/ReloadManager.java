package io.frebel.reload;

import io.frebel.ClassInner;
import io.frebel.FrebelClass;
import io.frebel.FrebelClassRegistry;
import io.frebel.FrebelJVM;
import io.frebel.bcp.AddForwardBCP;
import io.frebel.bcp.AddUidBCP;
import io.frebel.bcp.ByteCodeProcessor;
import io.frebel.bcp.RenameBCP;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public enum ReloadManager {
    INSTANCE;
    private ByteCodeProcessor renameBCP = new RenameBCP();
    private ByteCodeProcessor addForward = new AddForwardBCP();
    private ByteCodeProcessor uidBCP = new AddUidBCP();

    public ClassInner reload(ClassInner classInner/* 新版本的class文件 */) throws Exception {
        return reload(classInner, true);
    }

    public ClassInner reload(ClassInner classInner, boolean updateClassVersion) throws Exception {
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
            frebelClass = new FrebelClass(processed, classPool.getClassLoader());
            FrebelClassRegistry.register(className, frebelClass);
            redefineFlag = true;
        }
        // modify
        byte[] bytes = classInner.getBytes();
        bytes = renameBCP.process(frebelClass.getClassLoader(), bytes);
        bytes = addForward.process(frebelClass.getClassLoader(), bytes);
        bytes = uidBCP.process(frebelClass.getClassLoader(), bytes);
        ClassInner newClassInner = new ClassInner(bytes);
        // load new class
        Class aClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytes), false).toClass();
        // redefine old class, add Forward
        if (redefineFlag) {
            FrebelJVM.getInstrumentation().redefineClasses(new ClassDefinition(Class.forName(className), processed));
        }

        if (updateClassVersion) {
            frebelClass.addNewVersionClass(newClassInner);
        }

        return newClassInner;
    }

    public void loadNewClass(ClassInner... classInner) throws Exception {
        for (ClassInner inner : classInner) {
            ClassPool.getDefault().makeClass(new ByteArrayInputStream(inner.getBytes()), false).toClass();
        }
    }

    public void batchReload(ClassInner... classInners) throws Exception {
        if (classInners == null) {
            return;
        }
        // new method
        if (classInners.length == 1) {
            reload(classInners[0]);
            return;
        }

        List<ClassInner> modifyClasses = new ArrayList<>();
        List<MethodInfo> newMethods = new ArrayList<>();
        // extract new method from class file
        for (ClassInner classInner : classInners) {
            String originClassName = classInner.getOriginClassName();
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(originClassName);
            List<MethodInfo> oldMethods;
            if (frebelClass == null) {
                // 第一次修改
                CtClass ctClass = ClassPool.getDefault().get(classInner.getOriginClassName());
                CtMethod[] ctMethods = ctClass.getDeclaredMethods();
                CtConstructor[] constructors = ctClass.getDeclaredConstructors();
                oldMethods = new ArrayList<>(ctMethods.length);
                for (CtMethod ctMethod : ctMethods) {
                    if (!ctMethod.getName().contains("_$fr$")) {
                        oldMethods.add(new MethodInfo(classInner.getOriginClassName(), ctMethod.getName(), ctMethod.getSignature()));
                    }
                }
                for (CtConstructor constructor : constructors) {
                    oldMethods.add(new MethodInfo(classInner.getOriginClassName(), "<init>", constructor.getSignature()));
                }
            } else {
                ClassInner oldClassInner = frebelClass.getCurrentVersionClass();
                oldMethods = oldClassInner.getDeclaredMethods();
            }
            List<MethodInfo> _newMethods = new ArrayList<>(classInner.getDeclaredMethods());
            _newMethods.removeAll(oldMethods);
            newMethods.addAll(_newMethods);
            modifyClasses.add(classInner);
        }

        // search all references to new methods，replace the class name
        for (ClassInner classInner : classInners) {
            String className = classInner.getOriginClassName();
            List<MethodInfo> constantPoolMethods = classInner.getConstantPoolMethods();
            for (MethodInfo newMethod : newMethods) {
                if (Objects.equals(newMethod.getClassName(), className)) {
                    continue;
                }
                if (!constantPoolMethods.contains(newMethod)) {
                    continue;
                }

                FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(newMethod.getClassName());
                String newClassName;
                if (frebelClass == null) {
                    // class contains the new method was modified first time
                    newClassName = newMethod.getClassName() + "_$fr$_" + 1;
                } else {
                    newClassName = frebelClass.getNextVersionClassName();
                }
                // 修改使用到新方法的类名
                classInner.updateConstantMethodClassName(newMethod, newClassName);
                // 修改class中field类名
                classInner.updateConstantFieldClassName(newMethod.getClassName(), newClassName);
            }
        }

        // reload
        Map<String, ClassInner> finalClasses = new HashMap<>(modifyClasses.size());
        for (ClassInner modifyClass : modifyClasses) {
            finalClasses.put(modifyClass.getOriginClassName(), reload(modifyClass, false));
        }

        for (String className : finalClasses.keySet()) {
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
            frebelClass.addNewVersionClass(finalClasses.get(className));
        }
    }

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
