package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

import java.nio.ByteBuffer;

public class ConstantStringInfo extends CpInfo {
    private U2 utf8Index;

    public ConstantStringInfo(U1 tag) {
        super(tag);
    }

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), utf8Index.toBytes());
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        utf8Index = new U2(byteBuffer.get(), byteBuffer.get());
    }
}
