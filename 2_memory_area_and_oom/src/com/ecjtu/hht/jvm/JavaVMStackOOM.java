package com.ecjtu.hht.jvm;

/**
 * 建议别试 操作系统很卡
 * vm args:-Xss2M (这时候不妨设置大些)
 *
 * @author hht
 * @date 2020/6/18 14:18
 */
public class JavaVMStackOOM {
    private void dontStop() {
        while (true) {

        }
    }

    public void stackLeakByThread() {
        while (true) {
            Thread thread = new Thread(() -> dontStop());
            thread.start();
        }
    }

    public static void main(String[] args) {
        JavaVMStackOOM oom = new JavaVMStackOOM();
        oom.stackLeakByThread();
    }
}
