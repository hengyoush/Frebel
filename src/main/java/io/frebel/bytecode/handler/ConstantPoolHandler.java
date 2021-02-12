package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.ConstantDoubleInfo;
import io.frebel.bytecode.ConstantLongInfo;
import io.frebel.bytecode.CpInfo;
import io.frebel.bytecode.U1;
import io.frebel.bytecode.U2;

import java.nio.ByteBuffer;

public class ConstantPoolHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 2;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        U2 cpLength = new U2(byteBuffer.get(), byteBuffer.get());
        classFile.setConstantPoolCount(cpLength);
        int cpInfoLength = cpLength.toInt() - 1;
        classFile.setConstantPool(new CpInfo[cpInfoLength]);
        for (int i = 0; i < cpInfoLength; i++) {
            U1 tag = new U1(byteBuffer.get());
            CpInfo cpInfo = CpInfo.newCpInfo(tag);
            cpInfo.read(byteBuffer);
//            System.out.println("#" + (i + 1) + ":" + cpInfo);
            classFile.getConstantPool()[i] = cpInfo;
            if (cpInfo instanceof ConstantLongInfo || cpInfo instanceof ConstantDoubleInfo) {
                i++;
            }
        }
    }
}
