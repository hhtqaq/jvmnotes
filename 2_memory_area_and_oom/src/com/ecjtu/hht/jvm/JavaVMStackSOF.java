package com.ecjtu.hht.jvm;

/**
 * vm args: -Xss128k
 *
 * @author hht
 * @date 2020/6/18 14:00
 */
public class JavaVMStackSOF {
    private int stackLength = 1;

    public void stackLeak() {
        stackLength++;
        stackLeak();
    }

    public static void main(String[] args) {
        JavaVMStackSOF sof = new JavaVMStackSOF();
        try {
            sof.stackLeak();
        } catch (Throwable e) {
            System.out.println("stack length:" + sof.stackLength);
            throw e;
        }

    }
}
