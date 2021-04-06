package test.frebel.modify_args_num;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class ModifyArgsNumTest {
    @Test
    public void testModifyArgsNum() throws Exception {
        test.frebel.modify_args_num.A a = new test.frebel.modify_args_num.A();
        List<Object> test = a.test();
        Assert.assertTrue(test.size() == 1);
        Assert.assertTrue(test.get(0).equals("123"));
        reloadV2();
        test = a.test();
        Assert.assertTrue(test.size() == 2);
        Assert.assertTrue(test.get(0).equals("123"));
        Assert.assertTrue(test.get(1).equals(456));
    }

    public void reloadV2() throws Exception {
        String nameB = "./classfiles/modify-args-num/B_v2.bytecode";
        byte[] bytesB = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameB).toURI()));
        ClassInner classInnerB = new ClassInner(bytesB);
        String nameA = "./classfiles/modify-args-num/A_v2.bytecode";
        byte[] bytesA = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource(nameA).toURI()));
        ClassInner classInnerA = new ClassInner(bytesA);
        ReloadManager.INSTANCE.batchReload(classInnerB, classInnerA);
    }

    public static B b() {
        B b = new B(1, new B(), 3);
        b.t(1);
        return b;
//        return new B(new Integer(1));
    }
}
