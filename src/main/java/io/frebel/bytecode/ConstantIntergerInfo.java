package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantIntergerInfo extends CpInfo {
    private U4 bytes;

    public ConstantIntergerInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), bytes.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        this.bytes = new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get());
    }
}
