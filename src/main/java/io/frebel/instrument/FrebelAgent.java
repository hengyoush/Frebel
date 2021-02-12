package io.frebel.instrument;

import io.frebel.FrebelJVM;
import io.frebel.reload.FileChangeDetector;

import java.lang.instrument.Instrumentation;
import java.nio.file.Paths;

public class FrebelAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        FrebelJVM.setInstrumentation(instrumentation);
        String fileListenPath = Paths.get(".").toAbsolutePath() + "/" + "target/classes";
        FileChangeDetector fileChangeDetector = new FileChangeDetector(fileListenPath);
        System.out.println(fileListenPath);

        instrumentation.addTransformer(new FrebelTransformer());
        fileChangeDetector.start();
    }
}
