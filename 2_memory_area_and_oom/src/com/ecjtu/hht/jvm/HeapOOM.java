package com.ecjtu.hht.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * vm args: -Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 *
 * @author hht
 * @date 2020/6/18 13:39
 */
public class HeapOOM {
    static class OOMObject {

    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<OOMObject>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}
