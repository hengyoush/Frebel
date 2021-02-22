package io.frebel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

public class FrebelJVM {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrebelJVM.class);

    private static Instrumentation instrumentation;

    public static void setInstrumentation(Instrumentation i) {
        if (instrumentation == null) {
            instrumentation = i;
        } else {
            LOGGER.warn("Instrumentation already set!");
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
