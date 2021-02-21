package test;

import io.frebel.bcp.*;
import javassist.ClassPool;
import javassist.CtClass;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class A {
    private I b;
    private int a;
    public int asay() {
        Object b = (Object) new B(this);
        this.b = (I)b;
        if (b instanceof I) {
            System.out.println("1");
        }
        return (int) b;
    }

    public static void main(String[] args) throws Exception {
        CastAndInstanceOfBCP castAndInstanceOfBCP = new CastAndInstanceOfBCP();
        AddFieldAccessorBCP addFieldAccessorBCP = new AddFieldAccessorBCP();
        FieldRedirectBCP fieldRedirectBCP = new FieldRedirectBCP();
        AddForwardBCP addForwardBCP = new AddForwardBCP();
        MethodRedirectBCP methodRedirectBCP = new MethodRedirectBCP();

        CtClass ctClass = ClassPool.getDefault().get(A.class.getName());
        byte[] bytes = ctClass.toBytecode();

        byte[] processed = castAndInstanceOfBCP.process(Thread.currentThread().getContextClassLoader(), bytes);
        processed = addFieldAccessorBCP.process(Thread.currentThread().getContextClassLoader(), processed);
        processed = fieldRedirectBCP.process(Thread.currentThread().getContextClassLoader(), processed);
        processed = addForwardBCP.process(Thread.currentThread().getContextClassLoader(), processed);
        processed = methodRedirectBCP.process(Thread.currentThread().getContextClassLoader(), processed);
        Files.write(Paths.get("./test.class"), processed);
    }
}
