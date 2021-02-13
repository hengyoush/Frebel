package io.frebel.bytecode.handler;

import io.frebel.bytecode.AttributeInfo;
import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U2;
import io.frebel.bytecode.U4;

import java.nio.ByteBuffer;

public class AttributesHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 8;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setAttributesCount(new U2(byteBuffer.get(), byteBuffer.get()));
        Integer attributesCount = classFile.getAttributesCount().toInt();
        AttributeInfo[] attributeInfos = new AttributeInfo[attributesCount];
        for (int i = 0; i < attributesCount; i++) {
            AttributeInfo attributeInfo = new AttributeInfo();
            attributeInfo.setNameIndex(new U2(byteBuffer.get(), byteBuffer.get()));
            attributeInfo.setLength(new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
            byte[] bytes = new byte[attributeInfo.getLength().toInt()];
            byteBuffer.get(bytes, 0, bytes.length);
            attributeInfo.setInfo(bytes);
            attributeInfos[i] = attributeInfo;
        }
        classFile.setAttributes(attributeInfos);
    }
}
