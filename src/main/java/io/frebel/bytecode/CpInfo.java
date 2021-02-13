package io.frebel.bytecode;

public abstract class CpInfo implements ConstantInfoHandler {
    protected U1 tag;

    public CpInfo(U1 tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "tag=" + tag.toHexString();
    }

    public abstract byte[] toBytes();

    public static CpInfo newCpInfo(U1 tag) throws Exception {
        int tagValue = tag.toInt();
        CpInfo cpInfo;
        switch (tagValue) {
            case 1:
                cpInfo = new ConstantUtf8Info(tag);
                break;
            case 3:
                cpInfo = new ConstantIntergerInfo(tag);
                break;
            case 4:
                cpInfo = new ConstantFloatInfo(tag);
                break;
            case 5:
                cpInfo = new ConstantLongInfo(tag);
                break;
            case 6:
                cpInfo = new ConstantDoubleInfo(tag);
                break;
            case 7:
                cpInfo = new ConstantClassInfo(tag);
                break;
            case 8:
                cpInfo = new ConstantStringInfo(tag);
                break;
            case 9:
                cpInfo = new ConstantFieldInfo(tag);
                break;
            case 10:
                cpInfo = new ConstantMethodInfo(tag);
                break;
            case 11:
                cpInfo = new ConstantInterfaceMethodInfo(tag);
                break;
            case 12:
                cpInfo = new ConstantNameAndTypeInfo(tag);
                break;
            case 15:
                cpInfo = new ConstantMethodHandleInfo(tag);
                break;
            case 16:
                cpInfo = new ConstantMethodTypeInfo(tag);
                break;
            case 18:
                cpInfo = new ConstantInvokeDynamicInfo(tag);
                break;
            default:
                throw new Exception("No constant type tag=" + tag);
        }
        return cpInfo;
    }
}
