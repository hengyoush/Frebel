package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantInvokeDynamicInfo extends CpInfo {
    private U2 bootstrapMethodAttrIndex;
    private U2 nameAndTypeIndex;

    public ConstantInvokeDynamicInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        bootstrapMethodAttrIndex = new U2(byteBuffer.get(), byteBuffer.get());
        nameAndTypeIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }
}
