package io.frebel.bcp;

import io.frebel.ClassInner;

public interface ByteCodeProcessor {
    byte[] process(ClassLoader classLoader, byte[] bytes);
}
