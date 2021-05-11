package test.frebel.hierarchical.modify_super_class;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModifySuperClassTest {
    @Test
    public void testModifySuperClass() throws Exception {
        ModifySuperClassTest_A a = new ModifySuperClassTest_A();
        a.testMethod();
        reload("v2");
        a.testMethod();
    }

    public void reload(String version) throws Exception {
        String nameA = "./classfiles/modify-super-class/ModifySuperClassTest_A_"+version+".class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-super-class/ModifySuperClassTest_B_"+version+".class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
