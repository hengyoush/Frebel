package io.frebel.bytecode;

import java.util.HashMap;
import java.util.Map;

public class FieldAccessFlagUtils {
    private static final Map<Integer, String> fieldAccessFlagMap = new HashMap<>();

    static {
        fieldAccessFlagMap.put(0x0001, "public");
        fieldAccessFlagMap.put(0x0002, "private");
        fieldAccessFlagMap.put(0x0004, "protected");
        fieldAccessFlagMap.put(0x0008, "static");
        fieldAccessFlagMap.put(0x0100, "native");
        fieldAccessFlagMap.put(0x0010, "final");
        fieldAccessFlagMap.put(0x0040, "volatile");
        fieldAccessFlagMap.put(0x0080, "transient");
        fieldAccessFlagMap.put(0x1000, "synthetic");
        fieldAccessFlagMap.put(0x4000, "enum");
    }

    public static String toFieldAccessFlagString(U2 flag) {
        final int flagValue = flag.toInt();
        StringBuilder flagBuilder = new StringBuilder();
        for (Integer key : fieldAccessFlagMap.keySet()) {
            if ((flagValue & key) == key) {
                flagBuilder.append(fieldAccessFlagMap.get(key)).append(",");
            }
        }

        return flagBuilder.length() > 0 && flagBuilder.charAt(flagBuilder.length() - 1) == ','
                ?
                flagBuilder.substring(0, flagBuilder.length() - 1)
                :
                flagBuilder.toString();
    }

    public static boolean isPublic(int modifier) {
        return (modifier & 0x0001) == 0x0001;
    }

    public static boolean isNative(int modifier) {
        return (modifier & 0x0100) == 0x0100;
    }
}
