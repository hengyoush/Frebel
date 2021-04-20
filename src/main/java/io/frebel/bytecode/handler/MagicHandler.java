package io.frebel.bytecode.handler;

import io.frebel.bytecode.BaseByteCodeHandler;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.U4;

import java.nio.ByteBuffer;

public class MagicHandler implements BaseByteCodeHandler {
    @Override
    public int order() {
        return 0;
    }

    @Override
    public void read(ByteBuffer byteBuffer, ClassFile classFile) throws Exception {
        classFile.setMagic(new U4(byteBuffer.get(), byteBuffer.get(), byteBuffer.get(), byteBuffer.get()));
        if (!"0xCAFEBABE".equals(classFile.getMagic().toHexString())) {
            System.out.println(classFile.getMagic().toHexString());
            throw new Exception("not java class file!");
        }
    }
}
