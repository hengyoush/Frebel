package test.frebel.modify_return_type;

import org.junit.Assert;

public class IntToUserDefineTypeTest_A {
    IntToUserDefineTypeTest_B b = new IntToUserDefineTypeTest_B();

    public void testMethod() {
        Assert.assertTrue(b.testMethod() == 123);
    }
}
