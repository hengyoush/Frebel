package io.frebel.bytecode;

public class U4 {
    private final byte[] value;
    public U4(byte b1, byte b2, byte b3, byte b4) {
        this.value = new byte[] {b1, b2, b3, b4};
    }

    public int toInt() {
        int a = (value[0] & 0xff) << 24;
        a |= (value[1] & 0xff) << 16;
        a |= (value[2] & 0xff) << 8;
        a |= (value[3] & 0xff);
        return a;
    }

    public String toHexString() {
        char[] hexChar = new char[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder hexStr = new StringBuilder();
        for (int i = 3; i  >= 0; i--) {
            int v = value[i] & 0xff;
            while (v > 0) {
                int c = v % 16;
                v = v >>> 4;
                hexStr.insert(0, hexChar[c]);
            }
        }
        if ((hexStr.length()) == 1) {
            hexStr.insert(0, "0");
        }

        return "0x" + ((hexStr.length() == 0) ? "00" : hexStr.toString());
    }

    public byte[] toBytes() {
        return value;
    }
}
