package test.frebel.modify_return_type;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class IntToUserDefineTypeTest {
    @Test
    public void testIntToUserDefineType() throws Exception {
        IntToUserDefineTypeTest_A a = new IntToUserDefineTypeTest_A();
        a.testMethod();
        reload("v2");
        a.testMethod();
    }

    public void reload(String version) throws Exception {
        String nameA = "./classfiles/modify-return-type/IntToUserDefineTypeTest_A_"+version+".class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-return-type/IntToUserDefineTypeTest_B_"+version+".class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        String nameC = "./classfiles/modify-return-type/IntToUserDefineTypeTest_C_"+version+".class";
        byte[] bytesC = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameC).toURI()));
        ClassInner classInnerC = new ClassInner(bytesC);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB, classInnerC);
    }
}
