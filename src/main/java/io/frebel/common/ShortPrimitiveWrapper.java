package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class ShortPrimitiveWrapper implements PrimitiveWrapper {
    private short val;

    public ShortPrimitiveWrapper(short val) {
        this.val = val;
    }

    @Override
    public Short unwrap() {
        return val;
    }
}
