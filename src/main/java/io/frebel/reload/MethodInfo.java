package io.frebel.reload;

import io.frebel.bytecode.ConstantMethodInfo;

import java.util.Objects;

public class MethodInfo {
    private String className;
    private String name;
    private String descriptor;
    private ConstantMethodInfo constantMethodInfo;

    public MethodInfo(String className, String name, String descriptor) {
        this(className, name, descriptor, null);
    }

    public MethodInfo(String className, String name, String descriptor, ConstantMethodInfo constantMethodInfo) {
        this.className = className.replace("/", ".");
        this.name = name;
        this.descriptor = descriptor;
        this.constantMethodInfo = constantMethodInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(name, that.name) &&
                Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, name, descriptor);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public ConstantMethodInfo getConstantMethodInfo() {
        return constantMethodInfo;
    }
}
