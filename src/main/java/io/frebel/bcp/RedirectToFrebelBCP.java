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
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.LineNumberNode;
import jdk.internal.org.objectweb.asm.tree.LocalVariableNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static io.frebel.util.PrimitiveTypeUtil.getUnBoxedMethodSignature;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.ASM4;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.CHECKCAST;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.PUTFIELD;

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
                if (opcode == INVOKEVIRTUAL || opcode == INVOKEINTERFACE) { // TODO invokeinterface
                    try {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (skip(methodInsnNode)) { // skip frebel-generate method call
                            continue;
                        }
                        InsnList il = new InsnList();
                        il.add(new LdcInsnNode(methodInsnNode.name));
                        il.add(new LdcInsnNode(methodInsnNode.desc));

                        CtClass returnType = Descriptor.getReturnType(methodInsnNode.desc, ClassPool.getDefault());
                        String returnValueCastTo = getReturnValueCastTo(methodInsnNode, method);

                        il.add(new LdcInsnNode(returnValueCastTo));
                        int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                        il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getMethodName(paramNum), FrebelRuntime.getDesc(paramNum), false));
                        if (returnType != null && returnType.isPrimitive() && !"void".equals(returnType.getName())) {
                            String returnTypeName = PrimitiveTypeUtil.getBoxedClass(
                                    returnType.getName()).getName().replace(".", "/");
                            String castMethodName = returnType.getName() + "Value";
                            il.add(new TypeInsnNode(CHECKCAST, returnTypeName));
                            il.add(new MethodInsnNode(INVOKEVIRTUAL, returnTypeName, castMethodName, getUnBoxedMethodSignature(returnTypeName, castMethodName), false));
                        } else if (returnType != null && !returnType.isPrimitive()) {
                            // we are safe to use the returnValueCastTo to cast, because returnValueCastTo is not void and null
                            il.add(new TypeInsnNode(CHECKCAST, returnValueCastTo.replace(".", "/")));
                        }
                        insnList.insert(insnNode.getPrevious(), il);
                        insnList.remove(insnNode);
                        // we have pushed three value on the operand stack
                        method.maxStack += 3;
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

                    try {
                        String returnValueCastTo = getReturnValueCastTo(methodInsnNode, method);
                        il.add(new LdcInsnNode(returnValueCastTo));
                        int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                        il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getConsMethodName(paramNum), FrebelRuntime.getConsDesc(paramNum), false));
                        il.add(new TypeInsnNode(CHECKCAST, returnValueCastTo.replace(".", "/")));

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

    private String getReturnValueCastTo(MethodInsnNode methodInsnNode, MethodNode method) throws NotFoundException {
        String returnValueCastTo = "null";
        String returnTypeName;
        boolean isPrimitive ;
        if (methodInsnNode.name.equals("<init>")) {
            returnTypeName = methodInsnNode.owner.replace("/", ".");
            isPrimitive = false;
        } else {
            CtClass ctClass = Descriptor.getReturnType(methodInsnNode.desc, ClassPool.getDefault());
            if (ctClass == null) {
                returnTypeName = "void";
                isPrimitive = false;
            } else {
                returnTypeName = ctClass.getName();
                isPrimitive = ctClass.isPrimitive() && !ctClass.getName().equals("void");
            }
        }

        if ("void".equals(returnTypeName)) {
            // Void
            returnValueCastTo = "void";
        } else {
            // has returnType
            if (isPrimitive) {
                returnValueCastTo = "null";
            } else {
                AbstractInsnNode next = methodInsnNode.getNext();
                if (next instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) next;
                    // TODO PUTSTATIC
                    if (fieldInsnNode.getOpcode() == PUTFIELD) {
                        returnValueCastTo = Descriptor.getFieldDescReferenceClassName(fieldInsnNode.desc);
                    }
                } else if (next instanceof VarInsnNode) {
                    if (next.getOpcode() == ASTORE) {
                        VarInsnNode varInsnNode = (VarInsnNode) next;
                        int localVarIndex = varInsnNode.var;
                        // find local variable type in local variable node
                        List<LocalVariableNode> localVariables = method.localVariables;
                        List<LocalVariableNode> localVariableNodesWithSpecVar = new ArrayList<>();
                        for (int i = 0; i < localVariables.size(); i++) {
                            LocalVariableNode localVariableNode = localVariables.get(i);
                            if (localVariableNode.index == localVarIndex) {
                                localVariableNodesWithSpecVar.add(localVariableNode);
                            }
                        }
                        LabelNode[] lableBound = lableBound(next);
                        LabelNode start = lableBound[0];
                        LabelNode end = lableBound[1];
                        if (localVariableNodesWithSpecVar.get(0).start.getLabel() == end.getLabel()) {
                            // 使用第一个localVariableNode的类型信息
                            String desc = localVariableNodesWithSpecVar.get(0).desc;
                            returnValueCastTo = Descriptor.getFieldDescReferenceClassName(desc);
                        } else {
                            for (LocalVariableNode localVariableNode : localVariableNodesWithSpecVar) {
                                if (getLineNumberFromLableNode(localVariableNode.start) <= getLineNumberFromLableNode(start)
                                        && getLineNumberFromLableNode(localVariableNode.end) >= getLineNumberFromLableNode(end)) {
                                    returnValueCastTo = Descriptor.getFieldDescReferenceClassName(localVariableNode.desc);
                                    break;
                                }
                            }
                        }
                    }
                } else if (next instanceof InsnNode && next.getOpcode() == ARETURN) {
                    // get method return type
                    CtClass enclosingMethodReturnType = Descriptor.getReturnType(method.desc, ClassPool.getDefault());
                    if (enclosingMethodReturnType != null) {
                        returnValueCastTo = enclosingMethodReturnType.getName();
                    } else {
                        System.out.println("encounter areturn but method has no return type");
                    }
                } else {
                    returnValueCastTo = returnTypeName.replace(".", "/");
                }
            }
        }

        return returnValueCastTo.replace(".", "/");
    }

    private LabelNode[] lableBound(AbstractInsnNode methodInsnNode) {
        AbstractInsnNode cur = methodInsnNode.getPrevious();
        LabelNode start = null;
        while (true) {
            if (cur instanceof LabelNode) {
                start = ((LabelNode) cur);
                break;
            }

            if (cur.getPrevious() == null) {
                break;
            }

            cur = cur.getPrevious();
        }

        LabelNode end = null;
        cur = methodInsnNode.getNext();
        while (true) {
            if (cur instanceof LabelNode) {
                end = ((LabelNode) cur);
                break;
            }

            if (cur.getNext() == null) {
                break;
            }

            cur = cur.getNext();
        }

        return new LabelNode[]{start, end};
    }

    private int getLineNumberFromLableNode(LabelNode labelNode) {
        LineNumberNode lineNumberNode = (LineNumberNode) labelNode.getNext();
        return lineNumberNode.line;
    }
}
