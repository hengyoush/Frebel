package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantLongInfo extends CpInfo {
    private U4 hightBytes;
    private U4 lowBytes;

    public ConstantLongInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), hightBytes.toBytes(), lowBytes.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        hightBytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
        lowBytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
    }
}
