package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class LongPrimitiveWrapper implements PrimitiveWrapper {
    private long val;

    public LongPrimitiveWrapper(long val) {
        this.val = val;
    }

    @Override
    public Long unwrap() {
        return val;
    }
}
