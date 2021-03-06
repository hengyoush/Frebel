package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

public class AttributeInfo {
    private U2 nameIndex;
    private U4 length;
    private byte[] info;

    public byte[] toBytes() {
        return ArrayUtils.appendBytes(nameIndex.toBytes(), length.toBytes(), info);
    }

    public U2 getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(U2 nameIndex) {
        this.nameIndex = nameIndex;
    }

    public U4 getLength() {
        return length;
    }

    public void setLength(U4 length) {
        this.length = length;
    }

    public byte[] getInfo() {
        return info;
    }

    public void setInfo(byte[] info) {
        this.info = info;
    }
}
