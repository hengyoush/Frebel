package io.frebel.instrument;

import io.frebel.FrebelJVM;
import io.frebel.common.FrebelInitializeException;
import io.frebel.reload.FileChangeDetector;
import io.frebel.util.BannerPrinter;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;

public class FrebelAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        initLogger();
        Logger LOGGER = LoggerFactory.getLogger(FrebelAgent.class);
        BannerPrinter.print();

        FrebelJVM.setInstrumentation(instrumentation);

        String fileListenPath = Paths.get(".").toAbsolutePath() + "/" + "target/classes";
        FileChangeDetector fileChangeDetector = new FileChangeDetector(fileListenPath);
        fileChangeDetector.start();
        LOGGER.info("Start to listen file path: {}.", fileListenPath);

        instrumentation.addTransformer(new FrebelTransformer());
        LOGGER.info("Frebel agent init finished.");
    }

    private static void initLogger() {
        try {
            InputStream stream = FrebelAgent.class.getClassLoader().getResourceAsStream("frebel-log4j2.xml");
            if (stream == null) {
                throw new FrebelInitializeException("Frebel init failed, can't initialize log4j configurations.");
            }
            BufferedInputStream in = new BufferedInputStream(stream);
            final ConfigurationSource source = new ConfigurationSource(in);
            Configurator.initialize(null, source);
        } catch (Exception e) {
            throw new FrebelInitializeException(e);
        }
    }
}
