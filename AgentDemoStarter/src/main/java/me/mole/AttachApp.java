package me.mole;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.util.List;

public class AttachApp {

    public static void main(String[] args) {
        VirtualMachine vm = null;
        List<VirtualMachineDescriptor> listAfter = null;
        List<VirtualMachineDescriptor> listBefore = null;
        listBefore = VirtualMachine.list();
        while (true) {
            try {
                listAfter = VirtualMachine.list();
                if (listAfter.size() <= 0)
                    continue;
                for (VirtualMachineDescriptor vmd : listAfter) {
                    if ("me.mole.TargetAppMain".equals(vmd.displayName()) || "TargetApp.jar".equals(vmd.displayName())) {
                        vm = VirtualMachine.attach(vmd);
                        listBefore.add(vmd);
                        System.out.println("i find a vm, AgentDemo.jar was injected!");
                        Thread.sleep(1000);
                        if (null != vm) {
                            vm.loadAgent("/Users/fa1c0n/codeprojects/IdeaProjects/JavaInstrument/hook-test/AgentDemo.jar");
                            vm.detach();
                        }
                    }
                }
                break;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
