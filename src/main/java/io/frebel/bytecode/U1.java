package io.frebel.bytecode;

public class U1 {
    private final byte value;
    public U1(byte b1) {
        this.value = b1;
    }

    public int toInt() {
        return value & 0xff;
    }

    public String toHexString() {
        char[] hexChar = new char[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder hexStr = new StringBuilder();
        for (int i = 0; i  >= 0; i--) {
            int v = value & 0xff;
            while (v > 0) {
                int c = v % 16;
                v = v >>> 4;
                hexStr.insert(0, hexChar[c]);
                if ((hexStr.length() & 0x01) == 1) {
                    hexStr.insert(0, "0");
                }
            }
        }
        return "0x" + ((hexStr.length() == 0) ? "00" : hexStr.toString());
    }
}
