package test.frebel.add_method;

public class ModifyMethodName_A {
    ModifyMethodName_B b = new ModifyMethodName_B();
    public String testMethod() {
        return b.testMethod("123");
    }
}
