package test.frebel.modify_args_num;

import java.util.ArrayList;
import java.util.List;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class B {
    public B(){}
    public B(int i) {}
    public B(int a, B i, int j) {}

    public List<Object> test(String a) {
        List<Object> res = new ArrayList<Object>();
        res.add(a);
        return res;
    }

    public void t(int i) {
        Object[] objects = {1};
        return;
    }
}
