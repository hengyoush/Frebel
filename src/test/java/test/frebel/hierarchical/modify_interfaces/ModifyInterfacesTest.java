package test.frebel.hierarchical.modify_interfaces;

import io.frebel.ClassInner;
import io.frebel.reload.ReloadManager;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ModifyInterfacesTest {
    @Test
    public void testModifyInterfaces() throws Exception {
        Class.forName("test.frebel.hierarchical.modify_interfaces.ModifyInterfacesTest_A");
        ModifyInterfacesTest_A a = new ModifyInterfacesTest_A();
        a.testMethod();
        reload("v2");
        a.testMethod();
        reload("v3");
        a.testMethod();
        reload("v4");
        a.testMethod();
        reload("v5");
        a.testMethod();
        reload("v6");
        a.testMethod();
        reload("v7");
        a.testMethod();
//        reload("v8");
//        a.testMethod();
    }

    public void reload(String version) throws Exception {
        String classFilePath = "./classfiles/modify-interfaces/";
        List<ClassInner> classInners = Files.list(Paths.get(this.getClass().getClassLoader().getResource(classFilePath).toURI()))
                .filter(i -> i.toFile().getName().contains(version+".class"))
                .map(f -> {
                    try {
                        return Files.readAllBytes(f.toAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(ClassInner::new)
                .collect(Collectors.toList());
        ReloadManager.INSTANCE.batchReload(classInners.toArray(new ClassInner[]{}));
    }
}
