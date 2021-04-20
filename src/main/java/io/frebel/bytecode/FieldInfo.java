package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

public class FieldInfo {
    private U2 accessFlags;
    private U2 nameIndex;
    private U2 descriptorIndex;
    private U2 attributesCount;
    private AttributeInfo[] attributes;

    public byte[] toBytes() {
        byte[][] temp = new byte[attributes.length][];
        for (int i = 0; i < attributes.length; i++) {
            AttributeInfo attribute = attributes[i];
            temp[i] = attribute.toBytes();
        }
        byte[] attributeBytes = ArrayUtils.appendBytes(temp);
        return ArrayUtils.appendBytes(accessFlags.toBytes(), nameIndex.toBytes(),
                descriptorIndex.toBytes(), attributesCount.toBytes(), attributeBytes);
    }

    public U2 getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(U2 accessFlags) {
        this.accessFlags = accessFlags;
    }

    public U2 getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(U2 nameIndex) {
        this.nameIndex = nameIndex;
    }

    public U2 getDescriptorIndex() {
        return descriptorIndex;
    }

    public void setDescriptorIndex(U2 descriptorIndex) {
        this.descriptorIndex = descriptorIndex;
    }

    public U2 getAttributesCount() {
        return attributesCount;
    }

    public void setAttributesCount(U2 attributesCount) {
        this.attributesCount = attributesCount;
    }

    public AttributeInfo[] getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributeInfo[] attributes) {
        this.attributes = attributes;
    }
}
