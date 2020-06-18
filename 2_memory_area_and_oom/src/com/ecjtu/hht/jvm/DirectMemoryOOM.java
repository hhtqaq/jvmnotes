package com.ecjtu.hht.jvm;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 本机直接内存溢出  可通过 -XX:MaxDirectMemorySize
 *
 * 用unsafe直接分配本机内存
 * vm args: -Xmx20M -XX:MaxDirectMemorySize=10M
 * @author hht
 * @date 2020/6/18 15:00
 */
public class DirectMemoryOOM {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws IllegalAccessException {
        Field unsafeField = Unsafe.class.getDeclaredFields()[0];
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        for (;;){
            unsafe.allocateMemory(_1MB);
        }
    }
}
