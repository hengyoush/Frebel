package io.frebel;

import io.frebel.bytecode.ClassFile;
import io.frebel.bytecode.ClassFileAnalysis;

import java.util.Map;
import java.util.TreeMap;

public class FrebelClass {
    private String originName;
    private TreeMap<String, ClassInner> versionClassMap;
    private boolean isReloaded;
    // origin
    private byte[] originClassBytes;
    private ClassInner originClassInner;
    private ClassLoader classLoader;

    public FrebelClass(byte[] originClassBytes, ClassLoader classLoader) {
        this.originClassBytes = originClassBytes;
        this.isReloaded = false;
        this.versionClassMap = new TreeMap<>();
        this.classLoader = classLoader;
        try {
            this.originClassInner = new ClassInner(originClassBytes);
            this.versionClassMap.put(originClassInner.getOriginClassName(), originClassInner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateClass(String className, byte[] update) {
        // TODO
    }

    /**
     * 加载这个类
     *
     * @param classInner
     */
    public synchronized void addNewVersionClass(ClassInner classInner) {
        setReloaded(true);
        String className = classInner.getOriginClassName();
        versionClassMap.put(className, classInner);
    }

    public boolean isReloaded() {
        return isReloaded;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public int getNextVersion() {
        return versionClassMap.size();
    }

    public String getCurrentVersionClassName() {
        return versionClassMap.lastKey();
    }

    public synchronized void setReloaded(boolean reloaded) {
        isReloaded = reloaded;
    }
}
