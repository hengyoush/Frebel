package test.frebel.modify_return_type;

public class A {
    public Object testModifyReturnType() {
        B b = new B();
        return b.testModifyReturnType();
    }
}
