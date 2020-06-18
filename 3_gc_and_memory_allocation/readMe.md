## 第3章 垃圾收集器与内存分配策略

 ### 3.1 概述
 
&emsp;&emsp;垃圾收集主要关注三件事情：
 - 哪些内存需要回收？
 - 什么时候回收？
 - 如何回收?
 
&emsp;&emsp;程序计数器、虚拟机栈、本地方法栈3个区域属于**_线程私有_** ，随
线程生，随线程灭；这几个区域的内存分配和回收都具备确定性，因为，在编译期间就
能够确定下来不需要过多考虑回收的问题。而Java堆和方法区则不一样，一个接口中
的多个实现类需要的内存可能不一样，多态让我们只有在程序运行期间才能知道会创建
哪些对象，这部分内存的分配和回收都是动态的，垃圾收集器所关注的是这部分内存。

### 3.2 如何找到可回收的对象
&emsp;&emsp;找到可回收的内存，也就是要找到死去的对象（即不可能再被任何途径使用的对象）。
主要有以下这些方法：

#### 3.2.1  引用计数法

&emsp;&emsp;给对象中添加一个引用计数器，每当有一个地方应用它时，计数器值就加1；当引用失效，
计数器值就减1；任何时刻计数器为0的对象就是不可能再被使用的。<br/>
&emsp;&emsp;实现简单，判定效率也高，大部分情况下是一个不错的算法，但是由于存在互相
引用问题，主流的Java虚拟机里面没有使用引用计数算法来管理内存。<br/>

&emsp;&emsp;如以下程序ReferenceCountingGC所示：

         public static void testGC() {
        
                ReferenceCountingGC objA = new ReferenceCountingGC();
                ReferenceCountingGC objB = new ReferenceCountingGC();
                objA.instance = objB;
                objB.instance = objA;
                objA = null;
                objB = null;
                //假设在这行发生GC， objA和objB是否能被回收？
                System.gc();
           }

&emsp;&emsp;这两个对象互相引用着对方，即使改变指向，它们的引用计数都不为0，导致引用计数法无法回收。

#### 3.2.2 可达性分析算法
&emsp;&emsp;在主流的商用程序语言（Java、c#）的主流实现中，都是通过可达性分析来判定对象是否存活的。
这个算法的基本思路就是通过一系列的称为"GC Roots"的对象作为起始点，从这些节点开始向下搜索，搜索所
走过的路径称为引用链，当一个对象到GC Roots没有任何引用链相连，换句话说从GC Roots到这个对象不可达时，
则证明此对象是不可用的。如图3-1所示：对象object5、object6、object7虽然互相有关联，但是他们到GC Roots
是不可达的，所以他们将会被判定为可回收的对象。

![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/access_analyze.png)

&emsp;&emsp;在Java语言中，可以作为GC Roots的对象包括下面几种：
 - 虚拟机栈（栈帧中的本地变量表）中引用的对象。
 - 方法去中类静态属性引用的对象。
 - 方法区中常量引用的对象。
 - 本地方法栈中JNI（即一般说的Native方法）引用的对象。