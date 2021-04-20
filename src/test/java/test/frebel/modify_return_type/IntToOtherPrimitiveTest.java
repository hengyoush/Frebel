package test.frebel.modify_return_type;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/20
 */
public class IntToOtherPrimitiveTest {
    @Test
    public void testIntToOtherPrimitive() throws Exception {
        IntToOtherPrimitiveTest_A a = new IntToOtherPrimitiveTest_A();
        Assert.assertTrue(Objects.equals(123, a.test()));
        reload();
        Assert.assertTrue(Objects.equals('a', a.test()));
    }

    public void reload() throws Exception {
        String nameA = "./classfiles/modify-return-type/IntToOtherPrimitiveTest_A_v2.class";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);

        String nameB = "./classfiles/modify-return-type/IntToOtherPrimitiveTest_B_v2.class";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        ReloadManager.INSTANCE.batchReload(classInnerA, classInnerB);
    }
}
