package test.frebel.modify_return_value;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class ModifyReturnValueTest {
    @Test
    public void testModifyStringValue() throws Exception {
        A a = new A();
        Assert.assertEquals("123", a.testModifyStringValue());

        String name = "./classfiles/modify-return-value/A_v2.bytecode";
        byte[] bytes = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(name).toURI()));
        ClassInner classInner = new ClassInner(bytes);
        ReloadManager.INSTANCE.batchReload(classInner);

        Assert.assertEquals("456", a.testModifyStringValue());
        Files.deleteIfExists(Paths.get("./A.bytecode"));
    }

    @Test
    public void testModifyStringValueLambda() throws Exception {
        A a = new A();
        Supplier<String> testModifyStringValue = a::testModifyStringValue;
        Assert.assertEquals("123", testModifyStringValue.get());

        String name = "./classfiles/modify-return-value/A_v2.bytecode";
        byte[] bytes = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(name).toURI()));
        ClassInner classInner = new ClassInner(bytes);
        ReloadManager.INSTANCE.batchReload(classInner);
        Assert.assertEquals("456", testModifyStringValue.get());
    }

}
