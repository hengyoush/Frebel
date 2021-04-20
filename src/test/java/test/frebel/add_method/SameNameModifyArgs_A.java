package test.frebel.add_method;

public class SameNameModifyArgs_A {
    SameNameModifyArgs_B b = new SameNameModifyArgs_B();

    public String testMethod() {
        return b.testMethod("123");
    }
}
