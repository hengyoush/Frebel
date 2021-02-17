package io.frebel.bcp;

import io.frebel.bytecode.FieldAccessFlagUtils;
import io.frebel.util.Descriptor;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtPrimitiveType;
import javassist.Modifier;

import java.io.ByteArrayInputStream;

public class AddForwardBCP implements ByteCodeProcessor {
    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassPool classPool = ClassPool.getDefault();
        try {
            CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(bytes), false);
            if (ctClass.isInterface()) {
                return bytes;
            }
            // 增加forward处理
            CtMethod[] methods = ctClass.getMethods();
            boolean hasReturnType;
            boolean hasArgs;
            for (int i = 0; i < methods.length; i++) {
                CtMethod method = methods[i];
                if (FieldAccessFlagUtils.isPublic(method.getModifiers())
                        && !Modifier.isAbstract(method.getModifiers())
                        // 支持native方法的转移？
                        && !FieldAccessFlagUtils.isNative(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && !method.getName().contains("_$fr$")) {

                    CtClass returnType = method.getReturnType();
                    if ("void".equals(returnType.getName())) {
                        hasReturnType = false;
                    } else {
                        hasReturnType = returnType instanceof CtPrimitiveType || !returnType.getName().equals(Void.class.getName());
                    }
                    String returnTypeName = hasReturnType ? returnType.getName().replace("/", ".") : "void";
                    String[] parameterNames = Descriptor.getParameterNames(method.getSignature());
                    hasArgs = parameterNames != null && parameterNames.length > 0;
                    StringBuilder paramTypesBuilder = new StringBuilder();
                    paramTypesBuilder.append("new Class[]{");
                    for (int j = 0; hasArgs && j < parameterNames.length; j++) {
                        if (j != 0) {
                            paramTypesBuilder.append(",");
                        }
                        paramTypesBuilder.append(parameterNames[j]).append(".class");
                    }
                    paramTypesBuilder.append("}");

                    StringBuilder methodBuilder = new StringBuilder();
                    methodBuilder.append("Object _$frl$Cur=io.frebel.FrebelRuntime.getCurrentVersion(this);");
                    methodBuilder.append("if(_$frl$Cur!=$0){");
                    if (hasReturnType) {
                        methodBuilder.append("return (").append(returnTypeName).append(") ");
                    }
                    if (hasArgs) {
                        methodBuilder.append("io.frebel.FrebelRuntime.invokeWithParams(")
                                .append("\"").append(method.getName()).append("\"").append(",")
                                .append("$0").append(",")
                                .append("new Object[] {");
                        int paramsNum = parameterNames.length;
                        for (int j = 0; j < paramsNum; j++) {
                            if (j != 0) {
                                methodBuilder.append(",");
                            }
                            methodBuilder.append("$").append(j + 1);
                        }
                        methodBuilder.append("},")
                                .append(paramTypesBuilder.toString()).append(",")
                                .append("\"").append(returnTypeName).append("\"")
                                .append(");");
                    } else {
                        methodBuilder.append("io.frebel.FrebelRuntime.invokeWithNoParams(")
                                .append("\"").append(method.getName()).append("\"").append(",")
                                .append("$0").append(",")
                                .append("\"").append(returnTypeName).append("\"")
                                .append(");");
                    }
                    if (!hasReturnType) {
                        methodBuilder.append("return;");
                    }
                    methodBuilder.append("}");
                    try {
                        method.insertBefore(methodBuilder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
            return ctClass.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
