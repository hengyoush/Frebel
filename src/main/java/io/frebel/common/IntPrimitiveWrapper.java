package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class IntPrimitiveWrapper implements PrimitiveWrapper {
    private int val;

    public IntPrimitiveWrapper(int val) {
        this.val = val;
    }

    @Override
    public Integer unwrap() {
        return val;
    }
}
