package io.frebel.bcp;

import io.frebel.FrebelRuntime;
import io.frebel.util.Descriptor;
import io.frebel.util.PrimitiveTypeUtil;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ListIterator;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class CastAndInstanceOfBCP implements ByteCodeProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CastAndInstanceOfBCP.class);

    @Override
    public byte[] process(ClassLoader classLoader, byte[] bytes) {
        ClassNode cn = new ClassNode(ASM4);
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(cn, 0);
        List<MethodNode> methods = cn.methods;
        for (MethodNode method : methods) {
            if (method.name.startsWith("_$fr$_$s$") || method.name.startsWith("_$fr$_$g$")) {
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
                if (opcode != CHECKCAST && opcode != INSTANCEOF) {
                    continue;
                }
                TypeInsnNode typeInsnNode = (TypeInsnNode) insnNode;
                if (skip(typeInsnNode)) {
                    continue;
                }

                InsnList il = new InsnList();
                String desc = typeInsnNode.desc.replace("/", ".");
                il.add(new LdcInsnNode(desc));
                if (opcode == CHECKCAST) {
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeCast", FrebelRuntime.invokeCastDesc(), false));
                    il.add(new TypeInsnNode(CHECKCAST, typeInsnNode.desc));
                } else {
                    // instance of
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeInstanceOf", FrebelRuntime.invokeInstanceOfDesc(), false));
                }

                insnList.insert(insnNode.getPrevious(), il);
                insnList.remove(insnNode);
            }

            method.maxStack += 1;
        }
        ClassWriter classWriter = new ClassWriter(0);
        cn.accept(classWriter);
        return classWriter.toByteArray();
    }

    private boolean skip(TypeInsnNode insnNode) {
        String desc = insnNode.desc;
        AbstractInsnNode previous = insnNode.getPrevious();
        if (insnNode.getOpcode() == CHECKCAST && previous.getOpcode() == INVOKESTATIC) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) previous;
            if (methodInsnNode.owner.equals("io/frebel/FrebelRuntime")
                && methodInsnNode.name.equals("invokeCast")) {
                return true;
            }
        }

        try {
            CtClass ctClass = Descriptor.toCtClass(desc, ClassPool.getDefault());
            String className = ctClass.getName();
            try {
                String primitiveName = PrimitiveTypeUtil.getPrimitiveClassNameFromBoxClass(className);
                AbstractInsnNode next = insnNode.getNext();
                if (next != null && next.getOpcode() == INVOKEVIRTUAL) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                    String methodName = methodInsnNode.name;
                    if (methodName.equals(primitiveName + "Value")) {
                        return false;
                    } else {
                        return true;
                    }
                }
            } catch (IllegalArgumentException e) {
                // is not primitive box class
                if (className.startsWith("java") || className.startsWith("sun") || className.contains("io.frebel")) {
                    return true;
                }
            }
            return false;
        }catch (NotFoundException e) {
            LOGGER.error("class: {} not found!", insnNode.desc);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
