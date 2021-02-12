package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U2;

import java.nio.ByteBuffer;

public class InterfacesHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 5;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setInterfaceCount(new U2(byteBuffer.get(), byteBuffer.get()));
        Integer interfaceLength = classFile.getInterfaceCount().toInt();
        U2[] interfaces = new U2[interfaceLength];
        for (int i = 0; i < interfaceLength; i++) {
            interfaces[i] = new U2(byteBuffer.get(), byteBuffer.get());
        }
        classFile.setInterfaces(interfaces);
    }
}
