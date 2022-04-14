package me.mole;

public class TargetAppMain {

    public static void main(String[] args) throws Exception {
        System.out.println("Hi,guys! This is TargetApp.");

        Bird bird = new Bird();
        while (true) {
           bird.say();
           Thread.sleep(1000);
        }
    }
}
