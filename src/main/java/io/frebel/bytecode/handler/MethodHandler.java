package io.frebel.bytecode.handler;

import io.frebel.bytecode.AttributeInfo;
import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.MethodInfo;
import io.frebel.bytecode.U2;
import io.frebel.bytecode.U4;

import java.nio.ByteBuffer;

public class MethodHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 7;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setMethodCount(new U2(byteBuffer.get(), byteBuffer.get()));
        Integer len = classFile.getMethodCount().toInt();
        if (len == 0) {
            return;
        }

        MethodInfo[] methodInfos = new MethodInfo[len];
        classFile.setMethods(methodInfos);
        for (int i = 0; i < len; i++) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setAccessFlags(new U2(byteBuffer.get(), byteBuffer.get()));
            methodInfo.setNameIndex(new U2(byteBuffer.get(), byteBuffer.get()));
            methodInfo.setDescriptorIndex(new U2(byteBuffer.get(), byteBuffer.get()));
            methodInfo.setAttributesCount(new U2(byteBuffer.get(), byteBuffer.get()));
            int attributeCount = methodInfo.getAttributesCount();
            AttributeInfo[] attributeInfos = new AttributeInfo[attributeCount];
            for (int j = 0; j < attributeCount; j++) {
                AttributeInfo attributeInfo = new AttributeInfo();
                attributeInfos[j] = attributeInfo;
                attributeInfo.setNameIndex(new U2(byteBuffer.get(), byteBuffer.get()));
                attributeInfo.setLength(new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
                byte[] bytes = new byte[attributeInfo.getLength().toInt()];
                byteBuffer.get(bytes, 0, bytes.length);
                attributeInfo.setInfo(bytes);
            }
            methodInfo.setAttributeInfos(attributeInfos);
            methodInfos[i] = methodInfo;
        }
    }
}
