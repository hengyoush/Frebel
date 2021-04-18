package test.frebel.add_method;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModifyMethodNameTest {
    @Test
    public void testModifyMethodName() throws Exception {
        ModifyMethodName_A a = new ModifyMethodName_A();
        Assert.assertEquals("123", a.testMethod());
        reload();
        Assert.assertEquals("123", a.testMethod());
    }

    public void reload() throws Exception {
        String nameA = "./classfiles/add-method/ModifyMethodName_A_v2.class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/add-method/ModifyMethodName_B_v2.class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
