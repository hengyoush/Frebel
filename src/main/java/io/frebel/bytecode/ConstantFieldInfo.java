package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantFieldInfo extends CpInfo {
    private U2 classIndex;
    private U2 nameAndTypeIndex;

    public ConstantFieldInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), classIndex.toBytes(), nameAndTypeIndex.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        classIndex = new U2(byteBuffer.get(), byteBuffer.get());
        nameAndTypeIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }

    public int getClassIndex() {
        return classIndex.toInt();
    }

    public int getNameAndTypeIndex() {
        return nameAndTypeIndex.toInt();
    }
}
