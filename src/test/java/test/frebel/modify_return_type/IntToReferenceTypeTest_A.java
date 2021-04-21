package test.frebel.modify_return_type;

import org.junit.Assert;

public class IntToReferenceTypeTest_A {
    IntToReferenceTypeTest_B b = new IntToReferenceTypeTest_B();
    public void test() {
        int integer = b.testMethod();
        Assert.assertTrue(1 == integer);
    }
}
