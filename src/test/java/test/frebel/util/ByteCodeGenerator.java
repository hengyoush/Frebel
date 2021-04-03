package test.frebel.util;

import javax.tools.*;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ByteCodeGenerator {

    private static String basePath = "src/test/java/";
    private static String packagePath = "test/frebel/modify_return_value/";
    public static void generate(String name, String content) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        SimpleJavaFileObject javaFileObject = new StringSourceJavaObj(name, content);
        CharArrayWriter writer = new CharArrayWriter();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask compilerTask = compiler.getTask(writer, fileManager, diagnosticCollector, null, null, Arrays.asList(javaFileObject));
        Boolean result = compilerTask.call();
        if (!result) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
                System.out.println(diagnostic);
            }
            throw new IllegalArgumentException("Java source can't compile!");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Paths.get(".").toAbsolutePath());
        Path path = Paths.get("src/test/java/test/frebel/modify_return_value/v2/A.java");
        String collect = Files.readAllLines(path).stream().collect(Collectors.joining("\r\n"));
        System.out.println(collect);
        new ByteCodeGenerator().generate("test.frebel.modify_return_value.v2.A", collect);
        Files.readAllBytes(Paths.get(basePath, packagePath, "A.java"));
//        ReloadManager.INSTANCE.batchReload();
    }

    static class StringSourceJavaObj extends SimpleJavaFileObject {
        private String content;
        protected StringSourceJavaObj(String name, String content) {

            super(Paths.get(basePath , name.replace(".", "/") + ".java").toUri()
                    , Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }
    }
}
