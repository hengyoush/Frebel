package io.frebel;

public class FrebelProps {
    public static boolean debugClassFile() {
        return Boolean.valueOf(System.getProperty("debugClassFile", "false"));
    }
}
