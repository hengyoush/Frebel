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

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassDefinition;

public enum ReloadManager {
    INSTANCE;
    private ByteCodeProcessor renameBCP = new RenameBCP();
    private ByteCodeProcessor addForward = new AddForwardBCP();
    private ByteCodeProcessor uidBCP = new AddUidBCP();

    public void reload(ClassInner classInner/* 新版本的class文件 */) throws Exception {
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

        frebelClass.addNewVersionClass(newClassInner);
    }

    public void batchReload(ClassInner... classInners) throws Exception {
        // new method
        if (classInners != null && classInners.length == 1) {
            reload(classInners[0]);
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
