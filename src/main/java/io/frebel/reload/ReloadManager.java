package io.frebel.reload;

import io.frebel.ClassInner;
import io.frebel.FrebelClass;
import io.frebel.FrebelClassRegistry;
import io.frebel.FrebelJVM;
import io.frebel.FrebelProps;
import io.frebel.bcp.*;
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
    private ByteCodeProcessor castAndInstanceOfBCP = new CastAndInstanceOfBCP();

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
            if (!ctClass.isInterface() && !ctClass.isAnnotation()) {
                redefineFlag = true;
            } else {
                redefineFlag = false;
            }
        }
        // modify
        byte[] bytes = classInner.getBytes();
        bytes = renameBCP.process(frebelClass.getClassLoader(), bytes);
        bytes = castAndInstanceOfBCP.process(frebelClass.getClassLoader(), bytes);
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
}
