package io.frebel.bcp;

import io.frebel.FrebelRuntime;
import io.frebel.RedirectMethodGenerator;
import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static io.frebel.util.PrimitiveTypeUtil.getUnBoxedMethodSignature;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class MethodRedirectBCP implements ByteCodeProcessor {
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
                boolean flag = false;
                if (opcode == INVOKEVIRTUAL || opcode == INVOKEINTERFACE || opcode == INVOKESTATIC) {
                    try {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (skip(methodInsnNode)) { // skip frebel-generate method call
                            continue;
                        }
                        CtClass returnType = Descriptor.getReturnType(methodInsnNode.desc, ClassPool.getDefault());
                        String returnValueCastTo = getReturnValueCastTo(methodInsnNode, method);
                        int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                        InsnList il = new InsnList();

                        if (containsPrimitiveParam(methodInsnNode.desc)) {
                            String[] generate = RedirectMethodGenerator.getRedirectMethodInfo(methodInsnNode.desc);
                            il.add(new MethodInsnNode(INVOKESTATIC, generate[0], generate[1], generate[2], false));
                            il.add(new LdcInsnNode(methodInsnNode.name));
                            il.add(new LdcInsnNode(methodInsnNode.desc));
                            il.add(new LdcInsnNode(returnValueCastTo));
                            if (opcode == INVOKESTATIC) {
                                il.add(new LdcInsnNode(methodInsnNode.owner));
                                il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeStaticMethodsWithWrapperParams", "([Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
                            } else {
                                il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeInstanceMethodsWithWrapperParams", "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
                            }
                        } else {
                            il.add(new LdcInsnNode(methodInsnNode.name));
                            il.add(new LdcInsnNode(methodInsnNode.desc));
                            il.add(new LdcInsnNode(returnValueCastTo));
                            il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getMethodName(paramNum), FrebelRuntime.getDesc(paramNum), false));
                        }

                        if (returnType != null && returnType.isPrimitive() && !"void".equals(returnType.getName())) {
                            String returnTypeName = PrimitiveTypeUtil.getBoxedClass(
                                    returnType.getName()).getName().replace(".", "/");
                            String castMethodName = returnType.getName() + "Value";
                            il.add(new TypeInsnNode(CHECKCAST, returnTypeName));
                            il.add(new MethodInsnNode(INVOKEVIRTUAL, returnTypeName, castMethodName, getUnBoxedMethodSignature(returnTypeName, castMethodName), false));
                        } else if (returnType != null && "void".equals(returnType.getName())) {
                            il.add(new InsnNode(POP));
                        } else if (returnType != null && !returnType.isPrimitive()) {
                            // we are safe to use the returnValueCastTo to cast, because returnValueCastTo is not void and null
                            il.add(new TypeInsnNode(CHECKCAST, returnValueCastTo.replace(".", "/")));
                        }
                        insnList.insert(insnNode.getPrevious(), il);
                        insnList.remove(insnNode);
                        flag = true;
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
                    } else if ("<init>".equals(method.name) && (Objects.equals(cn.name, methodInsnNode.owner)
                        || Objects.equals(cn.superName, methodInsnNode.owner))) {
                        // skip when invoke cons or super cons within its own cons
                        // FIXME move to skip method
                        continue;
                    }

                    try {
                        String returnValueCastTo = getReturnValueCastTo(methodInsnNode, method);
                        int paramNum = Descriptor.numOfParameters(methodInsnNode.desc);
                        if (containsPrimitiveParam(methodInsnNode.desc)) {
                            String[] generate = RedirectMethodGenerator.getRedirectMethodInfo(methodInsnNode.desc);
                            il.add(new MethodInsnNode(INVOKESTATIC, generate[0], generate[1], generate[2], false));
                            il.add(new LdcInsnNode(methodInsnNode.owner));
                            il.add(new LdcInsnNode(methodInsnNode.desc));
                            il.add(new LdcInsnNode(returnValueCastTo));
                            il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeConsWithWrapperParams", "([Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
                        } else {
                            il.add(new LdcInsnNode(methodInsnNode.owner));
                            il.add(new LdcInsnNode(methodInsnNode.desc));
                            il.add(new LdcInsnNode(returnValueCastTo));
                            il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", FrebelRuntime.getConsMethodName(paramNum), FrebelRuntime.getConsDesc(paramNum), false));
                        }
                        il.add(new TypeInsnNode(CHECKCAST, returnValueCastTo.replace(".", "/")));

                        boolean newRemoved = false;
                        AbstractInsnNode cur = insnNode.getPrevious();
                        while ((!newRemoved) && cur != null) {
                            if (cur.getOpcode() != NEW) {
                                cur = cur.getPrevious();
                            } else if (cur.getOpcode() == NEW && methodInsnNode.owner.equals(((TypeInsnNode) cur).desc)) {
                                AbstractInsnNode previous = cur.getPrevious();
                                insnList.remove(cur.getNext()); // remove dup
                                insnList.remove(cur); // remove new
                                cur = previous;
                                newRemoved = true;
                            } else {
                                cur = cur.getPrevious();
                            }
                        }
                        insnList.insert(insnNode.getPrevious(), il);
                        // remove origin invoke instruction
                        insnList.remove(insnNode);
                        flag = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                // we have pushed three value on the operand stack
                if (flag) method.maxStack += 3;
            }
        }
        ClassWriter classWriter = new ClassWriter(0);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    private boolean skip(MethodInsnNode methodInsnNode) {
        if (/*methodInsnNode.owner.startsWith("java") ||*/
//                methodInsnNode.owner.startsWith("sun") ||
                        methodInsnNode.name.contains("_$fr$") ||
                        methodInsnNode.owner.contains("io/frebel")/* || Descriptor.numOfParameters(methodInsnNode.desc) > 10*/) {
            return true;
        }
        return false;
    }

    public boolean containsPrimitiveParam(String methodDesc) throws NotFoundException {

        CtClass[] parameterTypes = Descriptor.getParameterTypes(methodDesc, ClassPool.getDefault());
        return parameterTypes != null && parameterTypes.length > 0;
//        if (parameterTypes == null) {
//            return false;
//        }
//        return Arrays.stream(parameterTypes).anyMatch(CtClass::isPrimitive);
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
                returnValueCastTo = Object.class.getName();
//                returnValueCastTo = PrimitiveTypeUtil.getBoxedClass(returnTypeName).getName();
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
                        for (LocalVariableNode localVariableNode : localVariableNodesWithSpecVar) {
                            if (end == localVariableNode.start ||
                                    ( getInsnNodeIndex(localVariableNode.start) <= getInsnNodeIndex(next)
                                            && getInsnNodeIndex(localVariableNode.end) >= getInsnNodeIndex(next))) {
                                returnValueCastTo = Descriptor.getFieldDescReferenceClassName(localVariableNode.desc);
                                break;
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
                    returnValueCastTo = Object.class.getName();
//                    returnValueCastTo = returnTypeName.replace(".", "/");
                }
            }
        }

        return returnValueCastTo.equals("null") ?  Object.class.getName().replace(".", "/") :
                returnValueCastTo.replace(".", "/");
//        return returnValueCastTo.equals("null") ? returnTypeName.replace(".", "/") : returnValueCastTo.replace(".", "/");
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

    public int getInsnNodeIndex(AbstractInsnNode node) {
        try {
            Field index = AbstractInsnNode.class.getDeclaredField("index");
            index.setAccessible(true);
            return (int) index.get(node);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
