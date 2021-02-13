package io.frebel.bcp;

public interface ByteCodeProcessor {
    byte[] process(ClassLoader classLoader, byte[] bytes);
}
