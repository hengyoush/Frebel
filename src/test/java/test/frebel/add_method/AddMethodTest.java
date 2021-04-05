package test.frebel.add_method;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class AddMethodTest {
    @Test
    public void testModifyStringValue() throws Exception {
        A a = new A();
        Assert.assertEquals("B's originMethod", a.addMethodTest());

        String nameA = "./classfiles/add-method/A_v2.bytecode";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/add-method/B_v2.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);

        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);

        Assert.assertEquals("B's newMethod", a.addMethodTest());
    }
}
