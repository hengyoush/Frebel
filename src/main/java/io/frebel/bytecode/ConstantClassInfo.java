package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantClassInfo extends CpInfo {
    private U2 nameIndex;

    public ConstantClassInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), nameIndex.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        nameIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }

    public int getNameIndex() {
        return nameIndex.toInt();
    }
}
