package com.ecjtu.hht.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行时常量池溢出 jdk1.7以上 while循环将一直进行下去
 * vm args: -XX:PermSize=10M -XX:MaxPermSize=10M
 *
 * @author hht
 * @date 2020/6/18 14:33
 */
public class RuntimeConstantPoolOOM {
    public static void main(String[] args) {
        //使用List保持着常量池引用，避免Full GC回收常量池行为
        List<String> list = new ArrayList<>();
        //10MB 的permSize在integer范围内足够产生OOM了
        int i = 0;
        while (true) {
            list.add(String.valueOf(i++).intern());
        }

    }
}
