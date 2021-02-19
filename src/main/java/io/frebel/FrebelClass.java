package io.frebel;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.Map;
import java.util.TreeMap;

public class FrebelClass {
    private final String originName;
    private final TreeMap<String, ClassInner> versionClassMap;
    private boolean isReloaded;
    // origin
    private final byte[] originClassBytes;
    private final ClassInner originClassInner;
    private ClassLoader classLoader;

    // mutable
    private String superClassName;

    public FrebelClass(byte[] originClassBytes, ClassLoader classLoader) {
        this.originClassBytes = originClassBytes;
        this.isReloaded = false;
        this.versionClassMap = new TreeMap<>();
        this.classLoader = classLoader;
        try {
            this.originClassInner = new ClassInner(originClassBytes);
            this.originName = originClassInner.getOriginClassName();
            this.versionClassMap.put(originClassInner.getOriginClassName(), originClassInner);
            this.superClassName = originClassInner.getSuperClassName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateVersionClass(ClassInner classInner) {
        if (!versionClassMap.containsKey(classInner.getOriginClassName())) {
            throw new IllegalArgumentException("updateVersionClass error!");
        }

        versionClassMap.put(classInner.getOriginClassName(), classInner);
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
        this.superClassName = classInner.getSuperClassName();
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

    public String getSuperClassName() {
        return superClassName;
    }

    public String getOriginName() {
        return originClassInner.getOriginClassName();
    }

    public String getMatchedClassNameByParentClassName(String interfaceName) {
        String findName = null;
        interfaceName = interfaceName.replace("/", ".");
        try {
            CtClass ctClass = ClassPool.getDefault().get(interfaceName);
            for (Map.Entry<String, ClassInner> entry : versionClassMap.entrySet()) {
                ClassInner classInner = entry.getValue();
                CtClass ctClass2 = ClassPool.getDefault().get(classInner.getOriginClassName());
                if (ctClass2.subtypeOf(ctClass)) {
                    findName = entry.getKey();
                }
            }

            if (findName != null) {
                return findName;
            } else {
                throw new IllegalStateException("can't find class has implements the interface: " + interfaceName +
                        ",class name:" + originName);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getPreviousClassName(String className) {
        FrebelClass frebelClass = FrebelClassRegistry.getFrebelClass(className);
        int classIndex = Integer.parseInt(className.substring(className.lastIndexOf('_') + 1));
        String previousClassName;
        if (classIndex == 1) {
            previousClassName = frebelClass.getOriginName();
        } else {
            previousClassName = className.substring(0, className.lastIndexOf('_') + 1) + (classIndex - 1);
        }
        return previousClassName;
    }
}
