package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantNameAndTypeInfo extends CpInfo {
    private U2 nameIndex;
    private U2 descriptorIndex;

    public ConstantNameAndTypeInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), nameIndex.toBytes(), descriptorIndex.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        nameIndex = new U2(byteBuffer.get(), byteBuffer.get());
        descriptorIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }

    public int getNameIndex() {
        return nameIndex.toInt();
    }

    public int getDescriptorIndex() {
        return descriptorIndex.toInt();
    }
}
