package io.frebel;

public class FrebelProps {
    public static boolean debugClassFile() {
        return Boolean.parseBoolean(System.getProperty("debugClassFile", "false"));
    }
}
