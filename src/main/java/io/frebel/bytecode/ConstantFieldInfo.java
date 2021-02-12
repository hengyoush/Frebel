package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantFieldInfo extends CpInfo {
    private U2 classIndex;
    private U2 nameAndTypeIndex;

    public ConstantFieldInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        classIndex = new U2(byteBuffer.get(), byteBuffer.get());
        nameAndTypeIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }
}
