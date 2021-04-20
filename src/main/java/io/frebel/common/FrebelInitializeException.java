package io.frebel.common;

public class FrebelInitializeException extends RuntimeException {
    public FrebelInitializeException() {
        super();
    }

    public FrebelInitializeException(String msg) {
        super(msg);
    }

    public FrebelInitializeException(Throwable e) {
        super(e);
    }
}
