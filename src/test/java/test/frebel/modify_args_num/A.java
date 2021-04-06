package test.frebel.modify_args_num;

import java.util.List;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class A {
    B b = new B();
    public List<Object> test() {
        return b.test("123");
    }
}
