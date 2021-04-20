package io.frebel.bcp;

import io.frebel.FrebelClassRegistry;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;

public class RenameBCP implements ByteCodeProcessor {
    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassPool classPool = ClassPool.getDefault();
        try {
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes), false);
            ctClass.setName(FrebelClassRegistry.getFrebelClass(ctClass.getName()).getNextVersionClassName());
            return ctClass.toBytecode();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
