package io.frebel;

import io.frebel.util.Descriptor;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @version V1.0
 * @author yhheng
 * @date 2021/4/6
 */
public class RedirectMethodGenerator {
    private static Logger logger = LoggerFactory.getLogger(RedirectMethodGenerator.class);
    private static Map<String, String[]> cache = new HashMap<>();

    public static String[] getRedirectMethodInfo(String desc) {
        return cache.computeIfAbsent(desc, RedirectMethodGenerator::generate);
    }

    // 0-className,1-methodName,2-desc
    public static String[] generate(String desc) {
        try {
            CtClass[] parameterTypes = Descriptor.getParameterTypes(desc, ClassPool.getDefault());
            int numOfParameters = Descriptor.numOfParameters(desc);
            String className = generateClassName();
            String methodName = "invoke";

            CtClass ctClass = ClassPool.getDefault().makeClass("io.frebel." + className);
            List<String> parameterNames = new ArrayList<>();
            // FIXME when we reached 26 parameter names, we went to wrong!
            char c = 'a';
            Map<Character, String> isPrimitive = new HashMap<>();
            for (CtClass parameterType : Objects.requireNonNull(parameterTypes)) {
                parameterNames.add(parameterType.getName() + " " + c);
                if (parameterType.isPrimitive()) {
                    char[] chars = parameterType.getName().toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]); // int -> Int
                    isPrimitive.put(c, new String(chars));
                }
                c = (char) (c + 1);
            }
            StringBuilder method = new StringBuilder("public static Object[] invoke(" + String.join(",", parameterNames) + ")");
            method.append("{").append("return new Object[]{");
            char cur = 'a';
            for (int i = 0; i < numOfParameters; i++) {
                if (isPrimitive.containsKey(cur)) {
                    method.append("new ").append("io.frebel.common.").append(isPrimitive.get(cur)).append("PrimitiveWrapper").append("(").append(cur)
                            .append(")");
                } else {
                    method.append(cur);
                }
                cur = (char) (cur + 1);
                if (i < numOfParameters - 1) {
                    method.append(",");
                }
            }
            method.append("};}");
            CtMethod ctMethod = CtMethod.make(method.toString(), ctClass);
            ctClass.addMethod(ctMethod);
            if (FrebelProps.debugClassFile()) {
                ctClass.debugWriteFile();
            }
            ctClass.toClass();
            logger.warn("RedirectMethodGenerator generate: " + method.toString());
            return new String[] {ctClass.getName().replace(".", "/"), methodName, ctMethod.getSignature()};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateClassName() {
        // FIXME use more safe way
        return "Redirect" + System.currentTimeMillis();
    }
}
