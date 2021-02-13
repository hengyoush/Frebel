package io.frebel;

import java.util.TreeMap;

public class FrebelClass {
    private final String originName;
    private final TreeMap<String, ClassInner> versionClassMap;
    private boolean isReloaded;
    // origin
    private final byte[] originClassBytes;
    private final ClassInner originClassInner;
    private ClassLoader classLoader;

    public FrebelClass(byte[] originClassBytes, ClassLoader classLoader) {
        this.originClassBytes = originClassBytes;
        this.isReloaded = false;
        this.versionClassMap = new TreeMap<>();
        this.classLoader = classLoader;
        try {
            this.originClassInner = new ClassInner(originClassBytes);
            this.originName = originClassInner.getOriginClassName();
            this.versionClassMap.put(originClassInner.getOriginClassName(), originClassInner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    public ClassInner getCurrentVersionClass() {
        return versionClassMap.lastEntry().getValue();
    }

    public String getNextVersionClassName() {
        return  originName + ClassVersionManager.getReloadedClassPrefix(getNextVersion());
    }

    public synchronized void setReloaded(boolean reloaded) {
        isReloaded = reloaded;
    }
}
