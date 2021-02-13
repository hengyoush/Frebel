package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

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

    @Override
    public byte[] toBytes() {
        return ArrayUtils.appendBytes(tag.toBytes(), length.toBytes(), bytes);
    }

    public void update(String newStr) {
        this.bytes = newStr.getBytes(StandardCharsets.UTF_8);
        short l = (short) bytes.length;
        this.length = new U2((byte) ((l >>> 8) & 0xff), (byte) (l & 0xff));
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
