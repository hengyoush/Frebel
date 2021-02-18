package io.frebel.bcp;


import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.ListIterator;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM4;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
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
                if (opcode == PUTFIELD) {

                } else if (opcode == GETFIELD) {

                }
            }
        }


        return new byte[0];
    }
}
