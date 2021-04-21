package test.frebel.modify_return_type;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class IntToReferenceTypeTest {
    @Test
    public void testIntToReferenceType() throws Exception {
        IntToReferenceTypeTest_A a = new IntToReferenceTypeTest_A();
        a.test();
        reload("v2");
        a.test();
    }

    public void reload(String version) throws Exception {
        String nameA = "./classfiles/modify-return-type/IntToReferenceTypeTest_A_"+version+".class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-return-type/IntToReferenceTypeTest_B_"+version+".class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
