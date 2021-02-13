package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantMethodHandleInfo extends CpInfo {
    private U1 referenceKind;
    private U2 referenceIndex;


    public ConstantMethodHandleInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), referenceKind.toBytes(), referenceIndex.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        referenceKind = new U1(byteBuffer.get());
        referenceIndex = new U2(byteBuffer.get(), byteBuffer.get());
    }
}
