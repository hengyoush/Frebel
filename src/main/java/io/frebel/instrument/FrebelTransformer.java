package io.frebel.instrument;

import io.frebel.FrebelClass;
import io.frebel.FrebelClassRegistry;
import io.frebel.FrebelProps;
import io.frebel.bcp.*;
import io.frebel.common.FrebelInstrumentException;
import io.frebel.util.ClassUtil;
import javassist.ClassPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class FrebelTransformer implements ClassFileTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelTransformer.class);

    private final ByteCodeProcessor addUidBCP = new AddUidBCP();
    private final ByteCodeProcessor addFieldAccessorBCP = new AddFieldAccessorBCP();
    private ByteCodeProcessor redirectBCP = new MethodRedirectBCP();
    private ByteCodeProcessor castAndInstanceOfBCP = new CastAndInstanceOfBCP();

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        String dotClassName = className.replace("/", ".");
        try {
            if (loader == null || ClassUtil.needSkipTransform(className)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Frebel skip transform class first time, class name: {}.", dotClassName);
                }

                return classfileBuffer;
            }
            FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
            if (frebelClass == null) {
                byte[] processed = addFieldAccessorBCP.process(loader, classfileBuffer);
                processed = castAndInstanceOfBCP.process(loader, processed);
                processed = redirectBCP.process(loader, processed);
                processed = addUidBCP.process(loader, processed);
                if (FrebelProps.debugClassFile()) {
                    ClassPool.getDefault().makeClass(new ByteArrayInputStream(processed), false).debugWriteFile();
                }
                LOGGER.info("Frebel first transform class finished, class name: {}.", dotClassName);
                return processed;
            } else {
                return classfileBuffer;
            }
        } catch (Exception e) {
            LOGGER.error("Frebel transform class failed, class name: {}.", dotClassName, e);
            throw new FrebelInstrumentException(e);
        }
    }
}
