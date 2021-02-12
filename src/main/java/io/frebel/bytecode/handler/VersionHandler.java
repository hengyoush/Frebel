package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U2;

import java.nio.ByteBuffer;

public class VersionHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 1;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        U2 minorVersion = new U2(byteBuffer.get(), byteBuffer.get());
        classFile.setMinorVersion(minorVersion);
        U2 majorVersion = new U2(byteBuffer.get(), byteBuffer.get());
        classFile.setMajorVersion(majorVersion);
    }
}
