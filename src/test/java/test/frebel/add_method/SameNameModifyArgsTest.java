package test.frebel.add_method;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class SameNameModifyArgsTest {
    @Test
    public void testSameNameModifyArgs() throws Exception {
        SameNameModifyArgs_A a = new SameNameModifyArgs_A();
        Assert.assertEquals("123", a.testMethod());
        reload();
        Assert.assertEquals("123123", a.testMethod());
    }

    public void reload() throws Exception {
        String nameA = "./classfiles/add-method/SameNameModifyArgs_A_v2.class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/add-method/SameNameModifyArgs_B_v2.class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
