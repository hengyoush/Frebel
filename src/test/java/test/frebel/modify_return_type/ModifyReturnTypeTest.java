package test.frebel.modify_return_type;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModifyReturnTypeTest {
    @Test
    public void testModifyReturnValue() throws Exception {
        A a = new A();
        Object returnValue = a.testModifyReturnType();
        Assert.assertTrue(returnValue.equals("this"));
        reloadV2();
        returnValue = a.testModifyReturnType();
        Assert.assertTrue((Integer) returnValue == 123);
        reloadV3();
        returnValue = a.testModifyReturnType();
        Assert.assertTrue(returnValue instanceof B);
        reloadV4();
        returnValue = a.testModifyReturnType();
        Assert.assertTrue((Integer) returnValue == 123);
    }

    public int reloadV2() throws Exception {
        String nameB = "./classfiles/modify-return-type/B_v2.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.reload(classInnerB);
        return 1;
    }

    public int reloadV3() throws Exception {
        String nameB = "./classfiles/modify-return-type/B_v3.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.reload(classInnerB);
        return 2;
    }

    public int reloadV4() throws Exception {
        String nameB = "./classfiles/modify-return-type/B_v4.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.reload(classInnerB);
        return 2;
    }
}
