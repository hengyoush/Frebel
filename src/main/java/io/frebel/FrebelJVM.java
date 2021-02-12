package io.frebel;

import java.lang.instrument.Instrumentation;

public class FrebelJVM {
    private static Instrumentation instrumentation;

    public static void setInstrumentation(Instrumentation i) {
        if (instrumentation == null) {
            instrumentation = i;
        } else {
            System.out.println("Instrumentation already set!");
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
