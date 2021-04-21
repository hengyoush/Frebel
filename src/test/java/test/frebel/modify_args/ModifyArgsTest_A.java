package test.frebel.modify_args;

import org.junit.Assert;

public class ModifyArgsTest_A {
    ModifyArgsTest_B b = new ModifyArgsTest_B();
    public void testMethod() {
        Object[] objects = b.testMethod(1);
        Assert.assertTrue(objects[0].equals(1));
    }
}
