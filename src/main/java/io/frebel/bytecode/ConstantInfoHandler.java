package io.frebel.bytecode;

import java.nio.ByteBuffer;

public interface ConstantInfoHandler {
    void read(ByteBuffer byteBuffer) throws Exception;
}
