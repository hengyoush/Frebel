package io.frebel.bytecode;

import io.frebel.bytecode.handler.AccessFlagHandler;
import io.frebel.bytecode.handler.AttributesHandler;
import io.frebel.bytecode.handler.ConstantPoolHandler;
import io.frebel.bytecode.handler.FieldHandler;
import io.frebel.bytecode.handler.InterfacesHandler;
import io.frebel.bytecode.handler.MagicHandler;
import io.frebel.bytecode.handler.MethodHandler;
import io.frebel.bytecode.handler.ThisAndSuperHandler;
import io.frebel.bytecode.handler.VersionHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ClassFileAnalysis {
    private static List<BaseByteCodeHandler> handlers = new ArrayList<>();
    static {
        handlers.add(new MagicHandler());
        handlers.add(new VersionHandler());
        handlers.add(new ConstantPoolHandler());
        handlers.add(new AccessFlagHandler());
        handlers.add(new ThisAndSuperHandler());
        handlers.add(new InterfacesHandler());
        handlers.add(new FieldHandler());
        handlers.add(new MethodHandler());
        handlers.add(new AttributesHandler());
    }

    public static ClassFile analysis(byte[] bytes) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        ClassFile classFile = new ClassFile();
        for (BaseByteCodeHandler handler : handlers) {
            handler.read(byteBuffer, classFile);
        }
        return classFile;
    }

    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("/Users/hengyouhai/workspace/git/frebelcore/target/classes/io/frebel/bytecode/ClassAccessFlagsUtil.class"));
        ClassFile classFile = ClassFileAnalysis.analysis(bytes);
        System.out.println(classFile);
    }
}
