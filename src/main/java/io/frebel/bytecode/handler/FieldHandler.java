package io.frebel.bytecode.handler;

import io.frebel.bytecode.AttributeInfo;
import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.FieldInfo;
import io.frebel.bytecode.U2;
import io.frebel.bytecode.U4;

import java.nio.ByteBuffer;

public class FieldHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 6;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setFieldCount(new U2(byteBuffer.get(), byteBuffer.get()));
        int len = classFile.getFieldCount().toInt();
        if (len == 0) {
            return;
        }

        FieldInfo[] fieldInfos = new FieldInfo[len];
        classFile.setFields(fieldInfos);
        for (int i = 0; i < len; i++) {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfos[i] = fieldInfo;
            fieldInfo.setAccessFlags(new U2(byteBuffer.get(), byteBuffer.get()));
            fieldInfo.setNameIndex(new U2(byteBuffer.get(), byteBuffer.get()));
            fieldInfo.setDescriptorIndex(new U2(byteBuffer.get(), byteBuffer.get()));
            fieldInfo.setAttributesCount(new U2(byteBuffer.get(), byteBuffer.get()));
            AttributeInfo[] attributeInfos = new AttributeInfo[fieldInfo.getAttributesCount().toInt()];
            for (int j = 0; j < attributeInfos.length; j++) {
                AttributeInfo attributeInfo = new AttributeInfo();
                attributeInfos[j] = attributeInfo;
                attributeInfo.setNameIndex(new U2(byteBuffer.get(), byteBuffer.get()));
                attributeInfo.setLength(new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
                byte[] bytes = new byte[attributeInfo.getLength().toInt()];
                byteBuffer.get(bytes, 0, bytes.length);
                attributeInfo.setInfo(bytes);
            }
            fieldInfo.setAttributes(attributeInfos);
        }
    }
}
