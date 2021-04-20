package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class BooleanPrimitiveWrapper implements PrimitiveWrapper {
    private boolean val;

    public BooleanPrimitiveWrapper(boolean val) {
        this.val = val;
    }

    @Override
    public Boolean unwrap() {
        return val;
    }
}
