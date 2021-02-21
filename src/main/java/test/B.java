package test;

public class B implements I {
    public String c = "111";
    public B(A s) {

    }
    public void say() {

    }

    @Override
    public I sayI(I i) {
        System.out.println("say I");
        return this;
    }
}
