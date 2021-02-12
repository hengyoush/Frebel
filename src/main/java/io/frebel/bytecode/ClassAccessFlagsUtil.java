package io.frebel.bytecode;

import java.util.HashMap;
import java.util.Map;

public class ClassAccessFlagsUtil {
    private static final Map<Integer, String> classAccessFlagMap = new HashMap<>();

    static {
        // 公有类型
        classAccessFlagMap.put(0x0001, "public");
        // no sub classes
        classAccessFlagMap.put(0x0010, "final");
        classAccessFlagMap.put(0x0020, "super");
        classAccessFlagMap.put(0x0200, "interface");
        classAccessFlagMap.put(0x0400, "abstract");
        classAccessFlagMap.put(0x1000, "synthetic");
        classAccessFlagMap.put(0x2000, "annotation");
        classAccessFlagMap.put(0x4000, "enum");
    }

    public static String toClassAccessFlagsString(U2 flag) {
        final int flagValue = flag.toInt();
        StringBuilder flagBuilder = new StringBuilder();
        for (Integer key : classAccessFlagMap.keySet()) {
            if ((flagValue & key) == key) {
                flagBuilder.append(classAccessFlagMap.get(key)).append(",");
            }
        }
        return flagBuilder.length() > 0 && flagBuilder.charAt(flagBuilder.length() - 1) == ','
                ?
                flagBuilder.substring(0, flagBuilder.length() - 1)
                :
                flagBuilder.toString();
    }
}
