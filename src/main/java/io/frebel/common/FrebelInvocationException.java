package io.frebel.common;

public class FrebelInvocationException extends RuntimeException {

    public FrebelInvocationException() {
        super();
    }

    public FrebelInvocationException(String msg) {
        super(msg);
    }

    public FrebelInvocationException(Throwable e) {
        super(e);
    }
}
