package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantDoubleInfo extends CpInfo {
    private U4 hightBytes;
    private U4 lowBytes;

    public ConstantDoubleInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        hightBytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
        lowBytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
    }
}
