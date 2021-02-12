package io.frebel;

import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.ClassFileAnalysis;
import io.frebel.bytecode.ConstantClassInfo;
import io.frebel.bytecode.ConstantUtf8Info;
import io.frebel.bytecode.CpInfo;
import io.frebel.bytecode.U2;

public class ClassInner {
    private ClassFile classFile;
    private String originClassName;
    private String currentClassName;
    private String superClassName;
    private Integer constantPoolCount;
    private byte[] bytes;

    public ClassInner(byte[] bytes) {
        try {
            this.classFile = ClassFileAnalysis.analysis(bytes);
            this.bytes = bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getOriginClassName() {
        if (this.originClassName != null) {
            return originClassName;
        }
        U2 thisClass = classFile.getThisClass();
        CpInfo[] constantPool = classFile.getConstantPool();
        ConstantClassInfo thisClassInfo = (ConstantClassInfo) constantPool[thisClass.toInt() - 1];
        U2 classNameIndex = thisClassInfo.getNameIndex();
        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[classNameIndex.toInt() - 1];
        return (originClassName = classNameUtf8.asString().replace("/", "."));
    }

    public String getSuperClassName() {
        if (this.superClassName != null) {
            return superClassName;
        }
        U2 superClass = classFile.getSuperClass();
        CpInfo[] constantPool = classFile.getConstantPool();
        ConstantClassInfo superClassInfo = (ConstantClassInfo) constantPool[superClass.toInt()];
        U2 classNameIndex = superClassInfo.getNameIndex();
        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[classNameIndex.toInt()];
        return (superClassName = classNameUtf8.asString());
    }

    public int getConstantPoolCount() {
        if (this.constantPoolCount != null) {
            return this.constantPoolCount;
        }
        U2 constantPoolCount = classFile.getConstantPoolCount();
        return (this.constantPoolCount = constantPoolCount.toInt());
    }

    public ClassFile getClassFile() {
        return classFile;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
