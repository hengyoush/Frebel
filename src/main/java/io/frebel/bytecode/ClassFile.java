package io.frebel.bytecode;

public class ClassFile {
    private U4 magic;
    private U2 minorVersion;
    private U2 majorVersion;
    private U2 constantPoolCount;
    private CpInfo[] constantPool = new CpInfo[]{};
    private U2 accessFlags;
    private U2 thisClass;
    private U2 superClass;
    private U2 interfaceCount;
    private U2[] interfaces = new U2[]{};
    private U2 fieldCount;
    private FieldInfo[] fields = new FieldInfo[]{};
    private U2 methodCount;
    private MethodInfo[] methods = new MethodInfo[]{};
    private U2 attributesCount;
    private AttributeInfo[] attributes = new AttributeInfo[]{};

    public ClassFile() {
    }

    public U4 getMagic() {
        return magic;
    }

    public void setMagic(U4 magic) {
        this.magic = magic;
    }

    public U2 getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(U2 minorVersion) {
        this.minorVersion = minorVersion;
    }

    public U2 getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(U2 majorVersion) {
        this.majorVersion = majorVersion;
    }

    public U2 getConstantPoolCount() {
        return constantPoolCount;
    }

    public void setConstantPoolCount(U2 constantPoolCount) {
        this.constantPoolCount = constantPoolCount;
    }

    public CpInfo[] getConstantPool() {
        return constantPool;
    }

    public void setConstantPool(CpInfo[] constantPool) {
        this.constantPool = constantPool;
    }

    public U2 getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(U2 accessFlags) {
        this.accessFlags = accessFlags;
    }

    public U2 getSuperClass() {
        return superClass;
    }

    public void setSuperClass(U2 superClass) {
        this.superClass = superClass;
    }

    public U2 getInterfaceCount() {
        return interfaceCount;
    }

    public void setInterfaceCount(U2 interfaceCount) {
        this.interfaceCount = interfaceCount;
    }

    public U2[] getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(U2[] interfaces) {
        this.interfaces = interfaces;
    }

    public U2 getFieldCount() {
        return fieldCount;
    }

    public void setFieldCount(U2 fieldCount) {
        this.fieldCount = fieldCount;
    }

    public FieldInfo[] getFields() {
        return fields;
    }

    public void setFields(FieldInfo[] fields) {
        this.fields = fields;
    }

    public U2 getMethodCount() {
        return methodCount;
    }

    public void setMethodCount(U2 methodCount) {
        this.methodCount = methodCount;
    }

    public MethodInfo[] getMethods() {
        return methods;
    }

    public void setMethods(MethodInfo[] methods) {
        this.methods = methods;
    }

    public AttributeInfo[] getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributeInfo[] attributes) {
        this.attributes = attributes;
    }

    public U2 getThisClass() {
        return thisClass;
    }

    public void setThisClass(U2 thisClass) {
        this.thisClass = thisClass;
    }

    public U2 getAttributesCount() {
        return attributesCount;
    }

    public void setAttributesCount(U2 attributesCount) {
        this.attributesCount = attributesCount;
    }
}
