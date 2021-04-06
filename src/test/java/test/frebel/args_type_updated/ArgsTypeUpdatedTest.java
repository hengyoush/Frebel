package test.frebel.args_type_updated;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class ArgsTypeUpdatedTest {
    @Test
    public void testModifyArgsType() throws Exception {
        A a = new A();
        B b = new B("123");
        Assert.assertEquals("123", a.testA(b));
        reloadV2();
        b = new B("123");
        Assert.assertEquals("123-v2", a.testA(b));
    }

    public void reloadV2() throws Exception {
        String nameB = "./classfiles/args-type-updated/B_v2.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerB);
    }
}
