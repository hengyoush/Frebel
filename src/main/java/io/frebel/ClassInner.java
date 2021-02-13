package io.frebel;

import io.frebel.bytecode.AttributeInfo;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.ClassFileAnalysis;
import io.frebel.bytecode.ConstantClassInfo;
import io.frebel.bytecode.ConstantFieldInfo;
import io.frebel.bytecode.ConstantMethodInfo;
import io.frebel.bytecode.ConstantNameAndTypeInfo;
import io.frebel.bytecode.ConstantUtf8Info;
import io.frebel.bytecode.CpInfo;
import io.frebel.bytecode.FieldInfo;
import io.frebel.bytecode.U2;
import io.frebel.reload.MethodInfo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClassInner {
    private ClassFile classFile;
    private String originClassName;
    private List<MethodInfo> constantPoolMethods;
    private Integer constantPoolCount;
    private byte[] bytes;
    private volatile boolean modified;

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
        int classNameIndex = thisClassInfo.getNameIndex();
        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[classNameIndex - 1];
        return (originClassName = classNameUtf8.asString().replace("/", "."));
    }

//    public String getSuperClassName() {
//        if (this.superClassName != null) {
//            return superClassName;
//        }
//        U2 superClass = classFile.getSuperClass();
//        CpInfo[] constantPool = classFile.getConstantPool();
//        ConstantClassInfo superClassInfo = (ConstantClassInfo) constantPool[superClass.toInt()];
//        U2 classNameIndex = superClassInfo.getNameIndex();
//        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[classNameIndex.toInt()];
//        return (superClassName = classNameUtf8.asString());
//    }

    public List<MethodInfo> getDeclaredMethods() {
        return getDeclaredMethods(true);
    }

    public List<MethodInfo> getDeclaredMethods(boolean containConstructor) {
        io.frebel.bytecode.MethodInfo[] selfMethodArr = classFile.getMethods();
        if (selfMethodArr == null) {
            return new ArrayList<>(0);
        }
        List<MethodInfo> result = new ArrayList<>(selfMethodArr.length);
        CpInfo[] cpInfos = classFile.getConstantPool();
        for (io.frebel.bytecode.MethodInfo methodInfo : selfMethodArr) {
            int methodNameIndex = methodInfo.getNameIndex();
            int descriptorIndex = methodInfo.getDescriptorIndex();
            String methodName = ((ConstantUtf8Info) cpInfos[methodNameIndex - 1]).asString();
            if (!containConstructor && "<init>".equals(methodName)) {
                continue;
            }
            String descriptor = ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).asString();
            result.add(new MethodInfo(getOriginClassName(), methodName, descriptor));
        }

        return result;
    }

    public List<MethodInfo> getConstantPoolMethods() {
        if (constantPoolMethods != null) {
            return constantPoolMethods;
        }
        CpInfo[] cpInfos = classFile.getConstantPool();
        List<MethodInfo> result = new ArrayList<>();
        for (CpInfo cpInfo : cpInfos) {
            if (cpInfo instanceof ConstantMethodInfo) {
                ConstantMethodInfo cpMethodInfo = (ConstantMethodInfo) cpInfo;
                int classIndex = cpMethodInfo.getClassIndex();
                int nameAndTypeIndex = cpMethodInfo.getNameAndTypeIndex();

                ConstantClassInfo constantClassInfo = (ConstantClassInfo) cpInfos[classIndex - 1];
                ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) cpInfos[nameAndTypeIndex - 1];
                int classNameIndex = constantClassInfo.getNameIndex();
                int methodNameIndex = nameAndTypeInfo.getNameIndex();
                int descriptorIndex = nameAndTypeInfo.getDescriptorIndex();
                String className = ((ConstantUtf8Info) cpInfos[classNameIndex - 1]).asString();
                String methodName = ((ConstantUtf8Info) cpInfos[methodNameIndex - 1]).asString();
                String descriptor = ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).asString();
                result.add(new MethodInfo(className, methodName, descriptor, cpMethodInfo));
            }
        }

        return constantPoolMethods = result;
    }

    public synchronized void updateConstantFieldClassName(String originClassName, String newClassName) {
        CpInfo[] cpInfos = classFile.getConstantPool();
        for (CpInfo cpInfo : cpInfos) {
            if (cpInfo instanceof ConstantFieldInfo) {
                ConstantFieldInfo constantFieldInfo = (ConstantFieldInfo) cpInfo;
                ConstantClassInfo constantClassInfo = (ConstantClassInfo) cpInfos[constantFieldInfo.getClassIndex() - 1];
                ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) cpInfos[constantFieldInfo.getNameAndTypeIndex() - 1];
                String t = "L" + originClassName.replace(".", "/") + ";";
                String newType = "L" + newClassName.replace(".", "/") + ";";
                int classNameIndex = constantClassInfo.getNameIndex();
                int descriptorIndex = nameAndTypeInfo.getDescriptorIndex();
                String className = ((ConstantUtf8Info) cpInfos[classNameIndex - 1]).asString().replace("/", ".");
                String type = ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).asString();
                if (className.equals(getOriginClassName()) && type.equals(t)) {
                    ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).update(newType);
                    modified = true;
                }
            }
        }
    }

    public synchronized void updateConstantMethodClassName(MethodInfo methodInfo, String newClassName) {
        CpInfo[] cpInfos = classFile.getConstantPool();
        for (CpInfo cpInfo : cpInfos) {
            if (cpInfo instanceof ConstantMethodInfo) {
                ConstantMethodInfo constantMethodInfo = (ConstantMethodInfo) cpInfo;
                ConstantClassInfo constantClassInfo = (ConstantClassInfo) cpInfos[constantMethodInfo.getClassIndex() - 1];
                ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) cpInfos[constantMethodInfo.getNameAndTypeIndex() - 1];
                int classNameIndex = constantClassInfo.getNameIndex();
                int methodNameIndex = nameAndTypeInfo.getNameIndex();
                int descriptorIndex = nameAndTypeInfo.getDescriptorIndex();
                String className = ((ConstantUtf8Info) cpInfos[classNameIndex - 1]).asString().replace("/", ".");
                String name = ((ConstantUtf8Info) cpInfos[methodNameIndex - 1]).asString();
                String type = ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).asString();
                if (Objects.equals(className, methodInfo.getClassName()) &&
                        Objects.equals(name, methodInfo.getName()) &&
                        Objects.equals(type, methodInfo.getDescriptor())) {
                    ((ConstantUtf8Info) cpInfos[classNameIndex - 1]).update(newClassName.replace(".", "/"));
                    modified = true;
                    return;
                }
            }
        }
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

    public synchronized byte[] getBytes() {
        if (isModified()) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                // magic
                bos.write(classFile.getMagic().toBytes());
                // minor version
                bos.write(classFile.getMinorVersion().toBytes());
                // major version
                bos.write(classFile.getMajorVersion().toBytes());
                // class pool counter
                bos.write(classFile.getConstantPoolCount().toBytes());
                // constant pool
                for (CpInfo cpInfo : classFile.getConstantPool()) {
                    bos.write(cpInfo.toBytes());
                }
                // access flag
                bos.write(classFile.getAccessFlags().toBytes());
                // this class and super class
                bos.write(classFile.getThisClass().toBytes());
                bos.write(classFile.getSuperClass().toBytes());
                // interfaces count
                bos.write(classFile.getInterfaceCount().toBytes());
                // interface
                U2[] interfaces = classFile.getInterfaces();
                for (U2 u2 : interfaces) {
                    bos.write(u2.toBytes());
                }
                // fields count
                bos.write(classFile.getFieldCount().toBytes());
                // fields
                FieldInfo[] fields = classFile.getFields();
                for (FieldInfo field : fields) {
                    bos.write(field.toBytes());
                }
                // method count
                bos.write(classFile.getMethodCount().toBytes());
                // methods
                io.frebel.bytecode.MethodInfo[] methods = classFile.getMethods();
                for (io.frebel.bytecode.MethodInfo method : methods) {
                    bos.write(method.toBytes());
                }
                // attribute count
                bos.write(classFile.getAttributesCount().toBytes());
                // attributes
                AttributeInfo[] attributes = classFile.getAttributes();
                for (AttributeInfo attribute : attributes) {
                    bos.write(attribute.toBytes());
                }
                resetCache();
                this.bytes = bos.toByteArray();
                this.classFile = ClassFileAnalysis.analysis(bytes);
                if (this.modified) {
                    this.modified = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return bytes;
    }

    private void resetCache() {
        this.classFile = null;
        this.originClassName = null;
        this.constantPoolMethods = null;
        this.constantPoolCount = null;
    }

    public boolean isModified() {
        return modified;
    }
}
