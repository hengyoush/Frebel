package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U2;

import java.nio.ByteBuffer;

public class AccessFlagHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 3;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setAccessFlags(new U2(byteBuffer.get(), byteBuffer.get()));
    }
}
