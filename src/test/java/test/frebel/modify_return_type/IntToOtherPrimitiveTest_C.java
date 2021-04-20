package test.frebel.modify_return_type;

import org.junit.Assert;

import java.util.Objects;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/20
 */
public class IntToOtherPrimitiveTest_C {
    IntToOtherPrimitiveTest_A a = new IntToOtherPrimitiveTest_A();
    public void test() {
        Assert.assertTrue(Objects.equals(123, a.test()));
    }
}
