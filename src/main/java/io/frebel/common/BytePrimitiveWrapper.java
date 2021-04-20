package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class BytePrimitiveWrapper implements PrimitiveWrapper {
    private byte val;

    public BytePrimitiveWrapper(byte val) {
        this.val = val;
    }

    @Override
    public Byte unwrap() {
        return val;
    }
}
