package me.mole;

public class Shell {
    public static String execute(String cmd) throws Exception {
        System.out.println("cmd=" + cmd);
        String o = "";
        ProcessBuilder p;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            p = new ProcessBuilder(new String[]{"cmd.exe", "/c", cmd});
        } else {
            p = new ProcessBuilder(new String[]{"/bin/sh", "-c", cmd});
        }
        java.util.Scanner c = new java.util.Scanner(p.start().getInputStream()).useDelimiter("\\A");
        o = c.hasNext() ? c.next() : o;
        c.close();
        return o;
    }
}
