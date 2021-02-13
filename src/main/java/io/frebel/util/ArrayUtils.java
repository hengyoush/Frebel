package io.frebel.util;

public class ArrayUtils {
    public static byte[] appendBytes(byte[]... bytes) {
        int length = 0;
        for (byte[] aByte : bytes) {
            length += aByte.length;
        }
        byte[] result = new byte[length];
        int destPos = 0;
        for (byte[] aByte : bytes) {
            System.arraycopy(aByte, 0, result, destPos, aByte.length);
            destPos += aByte.length;
        }

        return result;
    }
}
