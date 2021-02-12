package io.frebel;

public class ClassVersionManager {
    public static String getReloadedClassPrefix(int version) {
        return "_$fr$_" + version;
    }
}
