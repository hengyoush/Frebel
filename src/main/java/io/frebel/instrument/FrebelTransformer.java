package io.frebel.instrument;

import io.frebel.FrebelClass;
import io.frebel.FrebelClassRegistry;
import io.frebel.bcp.AddFieldAccessorBCP;
import io.frebel.bcp.AddUidBCP;
import io.frebel.bcp.ByteCodeProcessor;
import io.frebel.bcp.MethodRedirectBCP;
import io.frebel.util.ClassUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class FrebelTransformer implements ClassFileTransformer {
    private ByteCodeProcessor addUidBCP = new AddUidBCP();
    private ByteCodeProcessor addFieldAccessorBCP = new AddFieldAccessorBCP();

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (loader == null || className.contains("_$fr$") || className.startsWith("io/frebel") || className.startsWith("java") || className.startsWith("sun")
                    ) {
                return classfileBuffer;
            }
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
            if (frebelClass == null) {
                // 初次加载类只增加uid and redirect
                byte[] processed = addFieldAccessorBCP.process(loader, classfileBuffer);
                processed = addUidBCP.process(loader, processed);
                return processed;
            } else {
                return classfileBuffer;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
