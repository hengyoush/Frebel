package io.frebel;

import io.frebel.bytecode.AttributeInfo;
import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.ClassFileAnalysis;
import io.frebel.bytecode.ConstantClassInfo;
import io.frebel.bytecode.ConstantNameAndTypeInfo;
import io.frebel.bytecode.ConstantUtf8Info;
import io.frebel.bytecode.CpInfo;
import io.frebel.bytecode.FieldInfo;
import io.frebel.bytecode.U2;
import io.frebel.common.FrebelClassFileAnalysisException;
import io.frebel.reload.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ClassInner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassInner.class);

    private ClassFile classFile;
    private String originClassName;
    private String superClassName;
    private Integer constantPoolCount;
    private byte[] bytes;
    private volatile boolean modified;

    public ClassInner(byte[] bytes) {
        try {
            this.classFile = ClassFileAnalysis.analysis(bytes);
            this.bytes = bytes;
        } catch (Exception e) {
            LOGGER.error("Error encounters when analysis class file bytes.", e);
            throw new FrebelClassFileAnalysisException(e);
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

    public String getSlashOriginClassName() {
        return getOriginClassName().replace(".", "/");
    }

    public String getSuperClassName() {
        if (this.superClassName != null) {
            return superClassName;
        }
        U2 superClass = classFile.getSuperClass();
        CpInfo[] constantPool = classFile.getConstantPool();
        if (superClass.toInt() == 0) {
            return null;
        }
        ConstantClassInfo superClassInfo = (ConstantClassInfo) constantPool[superClass.toInt() - 1];
        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[superClassInfo.getNameIndex() - 1];
        return (superClassName = classNameUtf8.asString());
    }

    public void updateSuperClassName(String newSuperClassName) {

        U2 superClass = classFile.getSuperClass();
        CpInfo[] constantPool = classFile.getConstantPool();
        ConstantClassInfo superClassInfo = (ConstantClassInfo) constantPool[superClass.toInt() - 1];
        ConstantUtf8Info classNameUtf8 = (ConstantUtf8Info) constantPool[superClassInfo.getNameIndex() - 1];
        classNameUtf8.update(newSuperClassName);
        this.superClassName = null;
        modified = true;
    }

    public List<String> getInterfaces() {
        U2[] interfacesIndex = classFile.getInterfaces();
        CpInfo[] cpInfos = classFile.getConstantPool();
        List<String> result = new ArrayList<>();
        for (U2 index : interfacesIndex) {
            ConstantClassInfo constantClassInfo = (ConstantClassInfo) cpInfos[index.toInt() - 1];
            String interfaceName = ((ConstantUtf8Info) cpInfos[constantClassInfo.getNameIndex() - 1]).asString().replace("/", ".");
            result.add(interfaceName);
        }

        return result;
    }

    public List<MethodInfo> getDeclaredMethods(boolean containConstructor, boolean notPrivate) {
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
            if (notPrivate && Modifier.isPrivate(methodInfo.getAccessFlags())) {
                continue;
            }
            String descriptor = ((ConstantUtf8Info) cpInfos[descriptorIndex - 1]).asString();
            result.add(new MethodInfo(getOriginClassName(), methodName, descriptor));
        }

        return result;
    }

    public void updateNameTypeAndClassInfo(String oldClassName, String newClassName) {
        CpInfo[] cpInfos = classFile.getConstantPool();
        String slashOldClassName = oldClassName.replace(".", "/");
        String slashNewClassName = newClassName.replace(".", "/");

        for (CpInfo cpInfo : cpInfos) {
            if (cpInfo instanceof ConstantNameAndTypeInfo) {
                ConstantNameAndTypeInfo nameAndTypeInfo = (ConstantNameAndTypeInfo) cpInfo;
                int descriptorIndex = nameAndTypeInfo.getDescriptorIndex();
                ConstantUtf8Info constantUtf8Info = (ConstantUtf8Info) cpInfos[descriptorIndex - 1];
                String s = constantUtf8Info.asString();
                if (s.contains(slashOldClassName)) {
                    constantUtf8Info.update(s.replace(slashOldClassName, slashNewClassName));
                    modified = true;
                }
            } else if (cpInfo instanceof ConstantClassInfo) {
                ConstantClassInfo constantClassInfo = (ConstantClassInfo) cpInfo;
                ConstantUtf8Info constantUtf8Info = (ConstantUtf8Info) cpInfos[constantClassInfo.getNameIndex() - 1];
                String s = constantUtf8Info.asString();
                if (s.contains(slashOldClassName)) {
                    constantUtf8Info.update(s.replace(slashOldClassName, slashNewClassName));
                    modified = true;
                }
            }
        }

        // modify field
        FieldInfo[] fields = classFile.getFields();
        for (FieldInfo field : fields) {
            ConstantUtf8Info constantUtf8Info = (ConstantUtf8Info) cpInfos[field.getDescriptorIndex().toInt() - 1];
            String s = constantUtf8Info.asString();
            if (s.contains(slashOldClassName)) {
                constantUtf8Info.update(s.replace(slashOldClassName, slashNewClassName));
                modified = true;
            }
        }

        // modify method params
        io.frebel.bytecode.MethodInfo[] methods = classFile.getMethods();
        for (io.frebel.bytecode.MethodInfo method : methods) {
            ConstantUtf8Info constantUtf8Info = (ConstantUtf8Info) cpInfos[method.getDescriptorIndex() - 1];
            String s = constantUtf8Info.asString();
            if (s.contains(slashOldClassName)) {
                constantUtf8Info.update(s.replace(slashOldClassName, slashNewClassName));
                modified = true;
            }
        }

        if (modified) {
            getBytes();
        }
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
                    if (cpInfo == null) continue;
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
                LOGGER.error(e.getMessage(), e);
                throw new FrebelClassFileAnalysisException(e);
            }
        }
        return bytes;
    }

    private void resetCache() {
        this.classFile = null;
        this.originClassName = null;
        this.constantPoolCount = null;
        this.superClassName = null;
    }

    public boolean isModified() {
        return modified;
    }
}
