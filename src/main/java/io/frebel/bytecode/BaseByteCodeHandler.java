package io.frebel.bytecode;

import java.nio.ByteBuffer;

public interface BaseByteCodeHandler {
    int order();
    void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception;
}
