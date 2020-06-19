package com.ecjtu.hht.jvm;

/**
 * vm args: -XX:+PrintGCDetails
 * 引用计数法的缺陷  循环引用无法回收内存
 *
 * @author hht
 * @date 2020/6/18 16:44
 */
public class ReferenceCountingGC {
    public Object instance = null;
    private static final int _1MB = 1024 * 1024;

    /**
     * 这个成员属性的唯一意义就是占点内存，以便能在gc日志中看清楚是否被回收过
     */
    private byte[] bigSize = new byte[2 * _1MB];

    public static void testGC() {

        ReferenceCountingGC objA = new ReferenceCountingGC();
        ReferenceCountingGC objB = new ReferenceCountingGC();
        //对象B被A引用了  计数器为1
        objA.instance = objB;
        //对象A被B引用了   计数器为1
        objB.instance = objA;
        //a对象引用改为null  按照道理需要回收内存了 但是由于A的引用计数不为0 被B引用了不回收
        objA = null;
        //b对象引用改为null  按照道理需要回收内存了 但是由于B的引用计数不为0 被A引用了不回收
        objB = null;
        //假设在这行发生GC， objA和objB是否能被回收？
        System.gc();
    }


    public static void testGcNoCycle() {
        ReferenceCountingGC objA = new ReferenceCountingGC();
        ReferenceCountingGC objB = new ReferenceCountingGC();
        //对象B被A引用了    计数器为1
        objA.instance = objB;
        //A=null 根据计数器为0  被回收了 回收之后a不在引用b  b的计数器变为0
        objA = null;
        //b=null  b的计数器为0  回收
        objB = null;
        System.gc();
    }


    public static void main(String[] args) {
        testGC();
    }
}
