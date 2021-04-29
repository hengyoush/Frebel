package test.frebel.modify_fields;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModifyFieldsTest {
    @Test
    public void testModifyFields() throws Exception {
        ModifyFieldsTest_A a = new ModifyFieldsTest_A();
        a.testMethod();
        reload("v2");
        a.testMethod();
    }

    public void reload(String version) throws Exception {
        String nameA = "./classfiles/modify-fields/ModifyFieldsTest_A_"+version+".class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-fields/ModifyFieldsTest_B_"+version+".class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
