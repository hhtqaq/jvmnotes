# 第3章 垃圾收集器与内存分配策略

## 3.1 概述
 
&emsp;&emsp;垃圾收集主要关注三件事情：
 - 哪些内存需要回收？
 - 什么时候回收？
 - 如何回收?
 
&emsp;&emsp;程序计数器、虚拟机栈、本地方法栈3个区域属于**_线程私有_** ，随
线程生，随线程灭；这几个区域的内存分配和回收都具备确定性，因为，在编译期间就
能够确定下来不需要过多考虑回收的问题。而Java堆和方法区则不一样，一个接口中
的多个实现类需要的内存可能不一样，多态让我们只有在程序运行期间才能知道会创建
哪些对象，这部分内存的分配和回收都是动态的，垃圾收集器所关注的是这部分内存。

## 3.2 如何找到可回收的对象
&emsp;&emsp;找到可回收的内存，也就是要找到死去的对象（即不可能再被任何途径使用的对象）。
主要有以下这些方法：

### 3.2.1  引用计数法

&emsp;&emsp;它的基本原理是，在每个对象中保存该对象的引用计数，当引用发生增减时对计
数进行更新。引用计数的增减，一般发生在变量赋值、对象内容更新、函数结束（局部变量不
再被引用）等时间点。当一个对象的引用计数变为0时，则说明它将来不会再被引用，因此可
以释放相应的内存空间。<br/>
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

### 3.2.2 可达性分析算法
&emsp;&emsp;在主流的商用程序语言（Java、c#）的主流实现中，都是通过可达性分析来判定对象是否存活的。
这个算法的基本思路就是通过一系列的称为"GC Roots"的对象作为起始点，从这些节点开始向下搜索，搜索所
走过的路径称为引用链，当一个对象到GC Roots没有任何引用链相连，换句话说从GC Roots到这个对象不可达时，
则证明此对象是不可用的。如图3-1所示：对象object5、object6、object7虽然互相有关联，但是他们到GC Roots
是不可达的，所以他们将会被判定为可回收的对象。

![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/access_analyze.png)

&emsp;&emsp;在Java语言中，可以作为GC Roots的对象包括下面几种：
 - 虚拟机栈（栈帧中的本地变量表）中引用的对象。
 - 方法区中类静态属性引用的对象。
 - 方法区中常量引用的对象。
 - 本地方法栈中JNI（即一般说的Native方法）引用的对象。
 
### 3.2.3 再谈引用
&emsp;&emsp;无论是通过引用计数算法判断对象的引用数量，还是通过可达性分析算法判断对象的引用链是否可达，
判定对象是否存活都与***“引用”***相关。在JDK1.2以前，Java中的引用的定义很传统：如果reference类型的数据
中存储的数值代表的是另外一块内存的起始地址，就称这块内存代表着一个引用。这种定义很纯粹，但是太过狭隘，
一个对象在这种定义下只有被引用或者没有被引用两种状态，对于如何描述一些“食之无味，弃之可惜”的对象就
显得无能为力。我们希望能描述这样一类对象：当内存空间还足够时，则能保留在内存之中；如果内存空间在进行
垃圾收集后还是非常紧张，则可以抛弃这些对象。很多系统的缓存功能都符合这样的应用场景。<br/>
&emsp;&emsp;在JDK1.2之后，Java对引用的概念进行了扩充，将引用分为强引用（Strong Reference）、软引用（Soft Reference）
、弱引用（Weak Reference）、虚引用（Phantom Reference）4种，这4种引用强度依次逐渐减弱。

 - ***强引用*** 就是指在程序代码之中普遍存在的，类似“Object obj=new Object()”这类的引用，只要强引用还存在，
 垃圾收集器永远不会回收掉被引用的对象。 
 - ***软引用*** 是用来描述一些还有用但并非必需的对象。对于软引用关联着的对象，在系统将要发生内存溢出异常之前
 将会把这些对象列进回收范围之中进行第二次回收。如果这次回收还没有足够的内存，才会抛出内存溢出一样。
 在JDK1.2之后，提供了SoftReference类来实现弱引用。
 - ***弱引用*** 也是用来描述非必需对象的，但是它的强度比软引用更弱一点，被弱引用关联的对象只能生存到下一次垃圾收集
 发生之前。当垃圾收集器工作时，无论当前内存是否足够，都会回收掉只被弱引用关联的对象。在JDK1.2之后，提供了
 WeakReference类来实现弱引用。
 - ***虚引用*** 也称为幽灵引用或者幻影引用，他是最弱的一种引用关系。一个对象是否有虚引用的存在，完全不会对其生存时间
 构成影响，也无法通过虚引用来取得一个对象实例。为一个对象设置虚引用关联的唯一目的就是能在这个对象被收集器回收
 时收到一个系统通知。在JDK1.2之后，提供了PhantomReference类来实现虚引用。
 
 ### 3.2.4 生存还是死亡
 &emsp;&emsp;即使在可达性分析算法中不可达的对象，也并非是“非死不可”的，这时候它
 们暂时处于“缓刑”阶段，要真正宣告一个对象死亡，至少要经历两次标记过程；如果对象
 在进行可达性分析后发现没有与GC Roots相连接的引用链，那它将会被第一次标记并且进行
 一次筛选，筛选的条件是此对象是否有必要执行finalize()方法。没有覆盖finalize()方法,
 或者finalize()方法已经被虚拟机调用过，虚拟机将这两种情况都视为“没有必要执行”。<br/>
 &emsp;&emsp;被判定为有必要执行finalize()方法的对象，会放在一个F-Queue队列中，并由低
 优先级的Finalizer线程去执行它。finalize()方法是对象逃脱死亡的最后一次机会，重新在方法
 中与引用链上的任何一个对象建立关联。如以下程序3-2：
 
    public class FinalizeEscapeGC {
    
    
        public static FinalizeEscapeGC SAVE_HOOK = null;
    
        public void isAlive() {
            System.out.println("yes,I am still alive;");
        }
    
        @Override
        protected void finalize() throws Throwable {
            System.out.println("finalize method executed");
            SAVE_HOOK = this;
    
        }
    
        public static void main(String[] args) throws Throwable {
    
            SAVE_HOOK = new FinalizeEscapeGC();
            //对象第一次成功拯救自己
            SAVE_HOOK = null;
            System.gc();
            //因为finalize方法的优先级很低  所以暂停1秒以等待它
            Thread.sleep(1000);
            if (SAVE_HOOK != null) {
                SAVE_HOOK.isAlive();
            } else {
                System.out.println("no , i am dead ;");
            }
            //下面这段代码与上面的完全相同，但是这次自救却失败了
            SAVE_HOOK=null;
            System.gc();
            //因为finalize方法的优先级很低  所以暂停1秒以等待它
            Thread.sleep(1000);
            if (SAVE_HOOK != null) {
                SAVE_HOOK.isAlive();
            } else {
                System.out.println("no , i am dead ;");
            }
        }
    }
 
 运行结果：
 
    finalize method executed
    yes,I am still alive;
    no , i am dead ;
  
&emsp;&emsp;从运行结果可以看出，SAVE_HOOK对象的finalize()方法确实被GC收集器触发过，并且由于重新指向，
在搜集前成功逃脱了。<br/>
&emsp;&emsp;另外值得注意的地方是，代码中有两段完全一样的代码片段，执行结果确实一次不被gc，一次gc，
这是因为任何一个对象的finalize()方法都只会被系统自动调用一次。<br />
&&emsp;&emsp;**建议**：<br />
建议大家忘掉这个方法。
### 3.2.5 回收方法区
&emsp;&emsp;很多人认为方法区（或者HotSpot虚拟机中的永久代）是没有垃圾收集的，Java虚拟机规范中
不要求虚拟机在方法区实现垃圾收集，因为垃圾收集的"性价比"一般比较低：在堆中，尤其是新生代中，
常规应用进行一次垃圾收集一般可以回收70%-95%的空间，而永久代的垃圾收集效率远低于此。<br />
&emsp;&emsp;永久代的垃圾收集主要回收两部分内容：废弃常量和无用的类。假如一个字符串"abc"已经进入了
常量池中，但是当前系统没有任何一个String对象是叫做"abc"的，如果此时发生内存回收，而且必要的话，
这个"abc"常量就会被系统清理出常量池。常量池中的其他类（接口）、方法、字段的符号引用也与此类似。<br />
&emsp;&emsp;类需要同时满足下面3个条件才能算是"无用的类":
 - 该类所有的实例都已经被回收， 也就是Java堆中不存在该类的任何实例。
 - 加载该类的ClassLoader已经被回收。
 - 该类对应的java.lang.Class 对象没有在任何地方被引用，无法在任何地方通过反射访问该类的方法。
&emsp;&emsp;虚拟机可以对满足上述3个条件的无用类进行回收，HotSpot虚拟机提供了-Xnoclassgc参数
进行控制。<br />
&emsp;&emsp;在大量使用反射、动态代理、CGLib等ByteCode框架、动态生成JSP以及OSGi这类频繁自定义
ClassLoader的场景都需要虚拟机具备类卸载的功能，以保证永久代不会溢出。
## 3.3 垃圾收集算法
&emsp;&emsp;前面解答了问题——哪些对象需要回收，但是还不能回答如何收集这个问题。**因为我们在程序
（程序也就是指我们运行在JVM上的JAVA程序）运行期间如果想进行垃圾回收，就必须让GC线程与程序当中
的线程互相配合，才能在不影响程序运行的前提下，顺利的将垃圾进行回收。**
### 3.3.1 标记——清除算法
&emsp;&emsp;为了达到这个目的，标记——清除算法就应运而生。**它的做法是当对中的有效内存空间被耗尽
的时候，就会停止整个程序（也被称为stop the world），然后进行两项工作，第一项则是标记，第二项则是
清除。**
最基础的收集算法是“标记——清除”（Mark-Sweep）算法，分为“标记”和“清除”两个阶段：
首先标记出所有需要回收的对象，在标记完成后统一回收所有被标记的对象，它的标记过程前面已经讲过了
引用计数法和可达性分析法。之所以说它是最基础的收集算法，是因为后续的收集算法都是基于这种思路，
并对其不足进行改进而得到的。主要不足有两个：
 - 效率问题，标记和清除两个过程的效率都不高；
 - 空间问题，标记清除之后会产生大量不连续的内存碎片，空间碎片太多可能会导致以后在程序运行
过程中需要分配较大对象时，无法找到足够的连续内存而不得不提前触发另一次垃圾收集动作。<br />
&emsp;&emsp;标记——清除算法的执行过程如图3-2所示。
 
 ![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/mark-sweep.png)
