package io.frebel.bytecode;

import java.nio.ByteBuffer;

public class ConstantClassInfo extends CpInfo {
    private U2 nameIndex;

    public ConstantClassInfo(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        nameIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }

    public U2 getNameIndex() {
        return nameIndex;
    }
}
