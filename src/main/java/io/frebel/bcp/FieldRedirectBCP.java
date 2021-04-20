package io.frebel.bcp;


import io.frebel.FrebelRuntime;
import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;
import java.util.ListIterator;

import static io.frebel.util.PrimitiveTypeUtil.getUnBoxedMethodSignature;
import static jdk.internal.org.objectweb.asm.Opcodes.ASM4;
import static jdk.internal.org.objectweb.asm.Opcodes.CHECKCAST;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.PUTFIELD;

public class FieldRedirectBCP implements ByteCodeProcessor {
    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassNode cn = new ClassNode(ASM4);
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(cn, 0);
        List<MethodNode> methods = cn.methods;
        for (MethodNode method : methods) {
            if (method.name.startsWith("_$fr$_$s$") || method.name.startsWith("_$fr$_$g$") ) {
                continue;
            }

            InsnList insnList = method.instructions;
            if (insnList == null || insnList.size() == 0) {
                continue;
            }

            ListIterator<AbstractInsnNode> iterator = insnList.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();
                int opcode = insnNode.getOpcode();
                if (!(insnNode instanceof FieldInsnNode)) {
                    continue;
                }
                // TODO 增加对静态变量访问的处理
                if (opcode != PUTFIELD && opcode != GETFIELD) {
                    continue;
                }
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                if (skip(fieldInsnNode)) {
                    continue;
                }

                String fieldOwner = fieldInsnNode.owner;
                String fieldName = fieldInsnNode.name;
                String fieldDesc = fieldInsnNode.desc;
                try {
                    CtClass returnCtClass = ClassPool.getDefault().get(Descriptor.toClassName(fieldDesc));
                    if (returnCtClass.isPrimitive()) {
                        fieldDesc = PrimitiveTypeUtil.getBoxedClass(returnCtClass.getName()).getName();
                    } else {
                        fieldDesc = Descriptor.toClassName(fieldDesc).replace("/", ".");
                    }
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                InsnList il = new InsnList();
                il.add(new LdcInsnNode(fieldOwner));
                il.add(new LdcInsnNode(fieldName));
                il.add(new LdcInsnNode(fieldDesc));
                if (opcode == PUTFIELD) {
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeSetField", FrebelRuntime.invokeSetFieldDesc(fieldInsnNode.desc), false));
                } else if (opcode == GETFIELD) {
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeGetField", FrebelRuntime.invokeGetFieldDesc(), false));
                }

                // add check cast
                if (opcode == GETFIELD) {
                    String fieldClassName = Descriptor.toClassName(fieldInsnNode.desc);
                    try {
                        CtClass ctClass = ClassPool.getDefault().get(fieldClassName);
                        if (ctClass.isPrimitive()) {
                            String returnTypeName = PrimitiveTypeUtil.getBoxedClass(fieldClassName).getName().replace(".", "/");
                            String castMethodName = fieldClassName + "Value";
                            il.add(new TypeInsnNode(CHECKCAST, returnTypeName));
                            il.add(new MethodInsnNode(INVOKEVIRTUAL, returnTypeName, castMethodName, getUnBoxedMethodSignature(returnTypeName, castMethodName), false));
                        } else {
                            il.add(new TypeInsnNode(CHECKCAST, fieldClassName.replace(".", "/")));
                        }
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }

                insnList.insert(insnNode.getPrevious(), il);
                insnList.remove(insnNode);
            }


            method.maxStack += 3;
        }

        ClassWriter classWriter = new ClassWriter(0);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    private boolean skip(FieldInsnNode insnNode) {
        String owner = insnNode.owner;
        if (owner.startsWith("java") || owner.startsWith("sun") || insnNode.name.contains("_$fr$_")) {
            return true;
        }

        return false;
    }
}
