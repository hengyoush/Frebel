package io.frebel.bcp;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 每个类都要在agent开始时加入uid方法
 * TODO 开启reuseOldObjectState选项
 */
public class AddUidBCP implements ByteCodeProcessor {
    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassPool classPool = ClassPool.getDefault();
        try {
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes), false);
            try {
                if (ctClass.getDeclaredField("_$fr$_uid") != null) {
                    return ctClass.toBytecode();
                }
            } catch (Exception ignore) {}
            if (ctClass.isInterface() || ctClass.isAnnotation()) {
                return bytes;
            }
            ctClass.addField(CtField.make("private final String _$fr$_uid=java.util.UUID.randomUUID().toString();", ctClass));
            ctClass.addMethod(CtMethod.make("public String _$fr$_getUid() {return this._$fr$_uid;}", ctClass));
            CtConstructor[] constructors = ctClass.getConstructors();
            for (CtConstructor constructor : constructors) {
                constructor.insertAfter("io.frebel.FrebelObjectManager.register(_$fr$_getUid(),$0);");
            }
            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
