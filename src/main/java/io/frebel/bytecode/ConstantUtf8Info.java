package io.frebel.bytecode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ConstantUtf8Info extends CpInfo {
    private U2 length;
    private byte[] bytes;

    public ConstantUtf8Info(U1 tag) {
        super(tag);
    }

    @Override
    public void read(ByteBuffer byteBuffer) throws Exception {
        length = new U2(byteBuffer.get(), byteBuffer.get());
        bytes = new byte[length.toInt()];
        byteBuffer.get(bytes, 0, length.toInt());
    }

    @Override
    public String toString() {
        return super.toString() + ",length=" + length.toInt()
                + ",str=" + new String(bytes, StandardCharsets.UTF_8);
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
