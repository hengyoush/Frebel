package io.frebel.bcp;

import io.frebel.FrebelRuntime;
import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
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
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;

public class RedirectToFrebelBCP implements ByteCodeProcessor {
    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassNode cn = new ClassNode(ASM4);
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(cn, 0);
        List<MethodNode> methods = cn.methods;
        for (MethodNode method : methods) {
            if (method.name.contains("_$fr$")) { // skip frebel-generate methods
                continue;
            }

            InsnList insnList = method.instructions;
            if (insnList.size() == 0) {
                continue;
            }

            ListIterator<AbstractInsnNode> iterator = insnList.iterator();
            // scan method call
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();
                int opcode = insnNode.getOpcode();
                if (opcode == INVOKEVIRTUAL) { // TODO invokespecial,invokeinterface
                    try {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (skip(methodInsnNode)) { // skip frebel-generate method call
                            continue;
                        }
                        InsnList il = new InsnList();
                        il.add(new LdcInsnNode(methodInsnNode.name));
                        il.add(new LdcInsnNode(methodInsnNode.desc));
                        int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                        il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getMethodName(paramNum), FrebelRuntime.getDesc(paramNum), false));

                        CtClass returnType = Descriptor.getReturnType(methodInsnNode.desc, ClassPool.getDefault());
                        if (returnType == null || "void".equals(returnType.getName())) {
                            // Void
                        } else {
                            // has returnType
                            if (returnType.isPrimitive()) {
                                String returnTypeName = PrimitiveTypeUtil.getBoxedClass(
                                        returnType.getName()).getName().replace(".", "/");
                                String castMethodName = returnType.getName() + "Value";
                                il.add(new TypeInsnNode(CHECKCAST, returnTypeName));
                                il.add(new MethodInsnNode(INVOKEVIRTUAL, returnTypeName, castMethodName, getUnBoxedMethodSignature(returnTypeName, castMethodName), false));
                            } else {
                                il.add(new TypeInsnNode(CHECKCAST, returnType.getName().replace(".", "/")));
                            }
                        }
                        insnList.insert(insnNode.getPrevious(), il);
                        insnList.remove(insnNode);
                        // we have pushed two value on the operand stack
                        method.maxStack += 2;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } else if (opcode == INVOKESPECIAL) {
                    // handle with constructor
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

                    InsnList il = new InsnList();
                    if (skip(methodInsnNode)) {
                        continue;
                    }

                    if (!methodInsnNode.name.equals("<init>")) {
                        continue;
                    }

                    il.add(new LdcInsnNode(methodInsnNode.owner));
                    il.add(new LdcInsnNode(methodInsnNode.desc));
                    int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getConsMethodName(paramNum), FrebelRuntime.getConsDesc(paramNum), false));

                    try {
                        il.add(new TypeInsnNode(CHECKCAST, methodInsnNode.owner.replace(".", "/")));

                        boolean dupRemoved = false;
                        boolean newRemoved = false;
                        AbstractInsnNode cur = insnNode.getPrevious();
                        while (!dupRemoved || !newRemoved) {
                            if (!dupRemoved && cur.getOpcode() != DUP) {
                                cur = cur.getPrevious();
                                continue;
                            } else if (!dupRemoved && cur.getOpcode() == DUP) {
                                AbstractInsnNode previous = cur.getPrevious();
                                insnList.remove(cur);
                                cur = previous;
                                dupRemoved = true;
                                continue;
                            }

                            if (!newRemoved && cur.getOpcode() != NEW) {
                                cur = cur.getPrevious();
                            } else if (!newRemoved && cur.getOpcode() == NEW) {
                                AbstractInsnNode previous = cur.getPrevious();
                                insnList.remove(cur);
                                cur = previous;
                                newRemoved = true;
                            }
                        }
                        insnList.insert(insnNode.getPrevious(), il);
                        // remove invokespecial
                        insnList.remove(insnNode);
                        // we have pushed two value on the operand stack
                        method.maxStack += 2;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        ClassWriter classWriter = new ClassWriter(0);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    private boolean skip(MethodInsnNode methodInsnNode) {
        if (methodInsnNode.owner.startsWith("java") ||
                methodInsnNode.owner.startsWith("sun")
                || methodInsnNode.name.contains("_$fr$")
                || methodInsnNode.owner.contains("io/frebel")) {
            return true;
        }
        return false;
    }
}
