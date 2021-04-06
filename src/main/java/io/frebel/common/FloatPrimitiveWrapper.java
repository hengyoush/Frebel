package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class FloatPrimitiveWrapper implements PrimitiveWrapper {
    private float val;

    public FloatPrimitiveWrapper(float val) {
        this.val = val;
    }

    @Override
    public Float unwrap() {
        return val;
    }
}
