package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantFloatInfo extends CpInfo {
    private U4 bytes;

    public ConstantFloatInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        this.bytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
    }
}
