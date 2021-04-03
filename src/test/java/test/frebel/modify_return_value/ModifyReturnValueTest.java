package test.frebel.modify_return_value;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;
import test.frebel.util.ByteCodeGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ModifyReturnValueTest {
    @Test
    public void testModifyStringValue() throws Exception {
        A a = new A();
        Assert.assertEquals("123", a.testModifyStringValue());
        // reload here
        Path path = Paths.get("src/test/java/test/frebel/modify_return_value/v2/A.java");
        String collect = Files.readAllLines(path).stream().collect(Collectors.joining("\r\n"));
        ByteCodeGenerator.generate("test.frebel.modify_return_value.v2.A",collect);
        ClassInner classInner = new ClassInner(Files.readAllBytes(Paths.get("./A.class")));
        ReloadManager.INSTANCE.batchReload(classInner);
        Assert.assertEquals("456", a.testModifyStringValue());
    }
}
