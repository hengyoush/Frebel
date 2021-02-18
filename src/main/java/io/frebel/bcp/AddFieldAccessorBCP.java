package io.frebel.bcp;

import io.frebel.util.PrimitiveTypeUtil;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * before add forward BCP
 */
public class AddFieldAccessorBCP implements ByteCodeProcessor {

    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass;
        try {
            ctClass = classPool.makeClass(new ByteArrayInputStream(bytes), false);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        CtField[] declaredFields = ctClass.getDeclaredFields();
        for (CtField declaredField : declaredFields) {
            boolean isPrimitive = false;
            String fieldName = declaredField.getName();
            String filedTypeName;
            try {
                filedTypeName = declaredField.getType().getName();
                if (declaredField.getType().isPrimitive()) {
                    filedTypeName = PrimitiveTypeUtil.getBoxedClass(filedTypeName).getName();
                    isPrimitive = true;
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            // add get method
            StringBuilder getBuilder = new StringBuilder();
            getBuilder.append("public ").append(filedTypeName)
                    .append(" _$fr$_$g$").append(fieldName)
                    .append("()").append("{");
            if (isPrimitive) {
                getBuilder.append("return ")
                        .append(filedTypeName).append(".valueOf($0.").append(fieldName).append(")")
                        .append(";");
            } else {
                getBuilder.append("return $0.").append(fieldName).append(";");
            }
            getBuilder.append("}");

            // add set method
            StringBuilder setBuilder = new StringBuilder();
            setBuilder.append("public void ").append("_$fr$_$s$").append(fieldName)
                    .append("(").append(filedTypeName).append(" o").append(")")
                    .append("{").append("$0.").append(fieldName).append("=");
            if (isPrimitive) {
                setBuilder.append("o." + PrimitiveTypeUtil.getPrimitiveClassNameFromBoxClass(filedTypeName))
                        .append("Value();");
            } else {
                setBuilder.append("o;");
            }
            setBuilder.append("}");

            try {
                ctClass.addMethod(CtMethod.make(getBuilder.toString(), ctClass));
                ctClass.addMethod(CtMethod.make(setBuilder.toString(), ctClass));
            } catch (CannotCompileException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        try {
            return ctClass.toBytecode();
        } catch (IOException | CannotCompileException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
