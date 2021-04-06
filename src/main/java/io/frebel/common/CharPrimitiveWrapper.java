package io.frebel.common;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class CharPrimitiveWrapper implements PrimitiveWrapper {
    private char val;

    public CharPrimitiveWrapper(char val) {
        this.val = val;
    }

    @Override
    public Character unwrap() {
        return val;
    }
}
