package test.frebel.modify_args;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModifyArgsTest {
    @Test
    public void testModifyArgs() throws Exception {
        ModifyArgsTest_A a = new ModifyArgsTest_A();
        a.testMethod();
        reload("v2");
        a.testMethod();
        reload("v3");
        a.testMethod();
    }

    public void reload(String version) throws Exception {
        String nameA = "./classfiles/modify-args/ModifyArgsTest_A_"+version+".class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-args/ModifyArgsTest_B_"+version+".class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
