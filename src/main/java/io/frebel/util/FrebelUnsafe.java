package io.frebel.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class FrebelUnsafe {
    private static Unsafe UNSAFE;
    private static Throwable UNSAFE_UNAVAILABILITY_CAUSE;
    static {
        final Object maybeUnsafe = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                    // We always want to try using Unsafe as the access still works on java9 as well and
                    // we need it for out native-transports and many optimizations.
                    unsafeField.setAccessible(true);
                    // the unsafe instance
                    return unsafeField.get(null);
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (SecurityException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                } catch (NoClassDefFoundError e) {
                    // Also catch NoClassDefFoundError in case someone uses for example OSGI and it made
                    // Unsafe unloadable.
                    return e;
                }
            }
        });

        if (maybeUnsafe instanceof Throwable) {
            UNSAFE = null;
            UNSAFE_UNAVAILABILITY_CAUSE = (Throwable) maybeUnsafe;
            System.out.println("sun.misc.Unsafe.theUnsafe: unavailable");
        } else {
            UNSAFE = (Unsafe) maybeUnsafe;
        }

        if (UNSAFE != null) {
            final Unsafe finalUnsafe = UNSAFE;
            final Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        finalUnsafe.getClass().getDeclaredMethod(
                                "allocateInstance", Class.class);
                        return null;
                    } catch (NoSuchMethodException e) {
                        return e;
                    } catch (SecurityException e) {
                        return e;
                    }
                }
            });

            if (maybeException == null) {
                System.out.println("sun.misc.Unsafe.allocateInstance: available");
            } else {
                UNSAFE = null;
                UNSAFE_UNAVAILABILITY_CAUSE = (Throwable) maybeException;
                System.out.println("sun.misc.Unsafe.allocateInstance: unavailable");
            }
        }
    }

    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
