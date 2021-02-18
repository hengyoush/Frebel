package io.frebel.bcp;


import io.frebel.FrebelRuntime;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FieldInsnNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.LdcInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.ListIterator;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM4;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
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
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
                if (fieldInsnNode.name.contains("_$fr$_")) {
                    continue;
                }

                String fieldOwner = fieldInsnNode.owner;
                String fieldName = fieldInsnNode.name;
                String fieldDesc = fieldInsnNode.desc;

                InsnList il = new InsnList();
                il.add(new LdcInsnNode(fieldOwner));
                il.add(new LdcInsnNode(fieldName));
                il.add(new LdcInsnNode(fieldDesc));
                if (opcode == PUTFIELD) {
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeSetField", FrebelRuntime.invokeSetFieldDesc(), false));
                } else if (opcode == GETFIELD) {
                    il.add(new MethodInsnNode(INVOKESTATIC, "io/frebel/FrebelRuntime", "invokeGetField", FrebelRuntime.invokeGetFieldDesc(), false));
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
}
