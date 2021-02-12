package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U2;

import java.nio.ByteBuffer;

public class ThisAndSuperHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 4;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setThisClass(new U2(byteBuffer.get(), byteBuffer.get()));
        classFile.setSuperClass(new U2(byteBuffer.get(), byteBuffer.get()));
    }
}
