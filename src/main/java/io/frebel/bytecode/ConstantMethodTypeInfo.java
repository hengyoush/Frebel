package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantMethodTypeInfo extends CpInfo {
    private U2 descriptorIndex;

    public ConstantMethodTypeInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        descriptorIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }
}
