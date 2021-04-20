package io.frebel.bytecode;

import io.frebel.util.ArrayUtils;

public class MethodInfo {
    private U2 accessFlags;
    private U2 nameIndex;
    private U2 descriptorIndex;
    private U2 attributesCount;
    private AttributeInfo[] attributeInfos;

    public byte[] toBytes() {
        byte[][] temp = new byte[attributeInfos.length][];
        for (int i = 0; i < attributeInfos.length; i++) {
            AttributeInfo attribute = attributeInfos[i];
            temp[i] = attribute.toBytes();
        }
        byte[] attributeBytes = ArrayUtils.appendBytes(temp);
        return ArrayUtils.appendBytes(accessFlags.toBytes(), nameIndex.toBytes(),
                descriptorIndex.toBytes(), attributesCount.toBytes(), attributeBytes);
    }

    public int getAccessFlags() {
        return accessFlags.toInt();
    }

    public void setAccessFlags(U2 accessFlags) {
        this.accessFlags = accessFlags;
    }

    public int getNameIndex() {
        return nameIndex.toInt();
    }

    public void setNameIndex(U2 nameIndex) {
        this.nameIndex = nameIndex;
    }

    public int getDescriptorIndex() {
        return descriptorIndex.toInt();
    }

    public void setDescriptorIndex(U2 descriptorIndex) {
        this.descriptorIndex = descriptorIndex;
    }

    public int getAttributesCount() {
        return attributesCount.toInt();
    }

    public void setAttributesCount(U2 attributesCount) {
        this.attributesCount = attributesCount;
    }

    public AttributeInfo[] getAttributeInfos() {
        return attributeInfos;
    }

    public void setAttributeInfos(AttributeInfo[] attributeInfos) {
        this.attributeInfos = attributeInfos;
    }
}
