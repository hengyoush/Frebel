package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class DoublePrimitiveWrapper implements PrimitiveWrapper {
    private double val;

    public DoublePrimitiveWrapper(double val) {
        this.val = val;
    }

    @Override
    public Double unwrap() {
        return val;
    }
}
