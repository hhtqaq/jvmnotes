package com.ecjtu.hht.jvm;

/**
 * 方法区内存溢出异常
 * vm args:  -XX:PermSize=10M -XX:MaxPermSize=10M
 *
 * @author hht
 * @date 2020/6/18 14:54
 */
public class JavaMethodAreaOOM {
    public static void main(String[] args) {
        while(true){
           // Enhancer
        }
    }
}
