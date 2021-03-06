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
以释放相应的内存空间。

&emsp;&emsp;实现简单，判定效率也高，大部分情况下是一个不错的算法，但是由于存在互相
引用问题，主流的Java虚拟机里面没有使用引用计数算法来管理内存。


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
垃圾收集后还是非常紧张，则可以抛弃这些对象。很多系统的缓存功能都符合这样的应用场景。

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
 或者finalize()方法已经被虚拟机调用过，虚拟机将这两种情况都视为“没有必要执行”。
 
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
在搜集前成功逃脱了。

&emsp;&emsp;另外值得注意的地方是，代码中有两段完全一样的代码片段，执行结果确实一次不被gc，一次gc，
这是因为任何一个对象的finalize()方法都只会被系统自动调用一次。

&emsp;&emsp;**建议**：

建议大家忘掉这个方法。
### 3.2.5 回收方法区
&emsp;&emsp;很多人认为方法区（或者HotSpot虚拟机中的永久代）是没有垃圾收集的，Java虚拟机规范中
不要求虚拟机在方法区实现垃圾收集，因为垃圾收集的"性价比"一般比较低：在堆中，尤其是新生代中，
常规应用进行一次垃圾收集一般可以回收70%-95%的空间，而永久代的垃圾收集效率远低于此。

&emsp;&emsp;永久代的垃圾收集主要回收两部分内容：废弃常量和无用的类。假如一个字符串"abc"已经进入了
常量池中，但是当前系统没有任何一个String对象是叫做"abc"的，如果此时发生内存回收，而且必要的话，
这个"abc"常量就会被系统清理出常量池。常量池中的其他类（接口）、方法、字段的符号引用也与此类似。

&emsp;&emsp;类需要同时满足下面3个条件才能算是"无用的类":
 - 该类所有的实例都已经被回收， 也就是Java堆中不存在该类的任何实例。
 - 加载该类的ClassLoader已经被回收。
 - 该类对应的java.lang.Class 对象没有在任何地方被引用，无法在任何地方通过反射访问该类的方法。
 
&emsp;&emsp;虚拟机可以对满足上述3个条件的无用类进行回收，HotSpot虚拟机提供了-Xnoclassgc参数
进行控制。

&emsp;&emsp;在大量使用反射、动态代理、CGLib等ByteCode框架、动态生成JSP以及OSGi这类频繁自定义
ClassLoader的场景都需要虚拟机具备类卸载的功能，以保证永久代不会溢出。
## 3.3 垃圾收集算法
&emsp;&emsp;前面解答了问题——哪些对象需要回收，但是还不能回答如何收集这个问题。**因为我们在程序
（程序也就是指我们运行在JVM上的JAVA程序）运行期间如果想进行垃圾回收，就必须让GC线程与程序当中
的线程互相配合，才能在不影响程序运行的前提下，顺利的将垃圾进行回收。**
### 3.3.1 标记——清除算法
&emsp;&emsp;为了达到这个目的，标记——清除算法就应运而生。**它的做法是当内存中的有效内存空间被耗尽
的时候，就会停止整个程序（也被称为stop the world），然后进行两项工作，第一项则是标记，第二项则是
清除。**
&emsp;&emsp;其实这两个步骤并不是特别复杂，也很容易理解。通俗的话解释一下标记/清除算法，就是当程序运行期间
，若可以使用的内存被耗尽的时候，GC线程就会被触发并将程序暂停，随后将依旧存活的对象标记一遍，最终
再将堆中所有没被标记的对象全部清除掉，接下来便让程序恢复运行。之所以说它是最基础的收集算法，
是因为后续的收集算法都是基于这种思路，并对其不足进行改进而得到的。主要不足有两个：
 - 效率问题，标记和清除两个过程的效率都不高；主要由于垃圾收集器需要从GC Roots根对象中遍历所
 有可达的对象，并给这些对象加上一个标记，表明此对象不会被清除，然后在清除阶段，垃圾
 收集器会从Java堆中从头到尾进行遍历，如果有对象没有被打上标记，那么这个对象就会被清除。
 显然遍历的效率是很低的。
 - 空间问题，标记清除之后会产生大量不连续的内存碎片，空间碎片太多可能会导致以后在程序运行
过程中需要分配较大对象时，无法找到足够的连续内存而不得不提前触发另一次垃圾收集动作。

&emsp;&emsp;标记——清除算法的执行过程如图3-2所示。

 ![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/mark-sweep.png)

### 3.3.2 复制算法
&emsp;&emsp;为了解决效率问题，一种称为“复制”（Copying）的收集算法出现了，它将可用内存按容量
划分为大小相等的两块，每次只是用其中的一块。当这一块的内存用完了，就将还存活着的对象复制到
另外一块上面，然后再把已使用过的内存空间一次清理掉。

&emsp;&emsp;优点：每次都是对整个半区进行内存回收，内存分配时也就不用考虑内存碎片等复杂情况，
只要移动堆顶的指针，按顺序分配内存即可，实现简单，运行高效。

&emsp;&emsp;缺点：代价是将内存缩小为原来的一半，未免太高了一些。复制算法的执行过程如图3-3：

 ![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/copy.png)
 
&emsp;&emsp;现在的商业虚拟机都采用这种收集算法来回收新生代，因为新生代中的对象98%是
“朝生夕死”的，所以并不需要按照1：1的比例来划分内存空间（只需要留出一点区域来保留存活的
对象），而是将内存分为一块较大的Eden空间和两块较小的Survivor空间，每次使用Eden和其中一块
Survivor。

&emsp;&emsp;当回收时，将Eden和Survivor中还存活着的对象一次性复制到另一块Survivor空间上
最后清理掉Eden和钢材使用过的Survivor空间。

&emsp;&emsp;HotSpot虚拟机默认Eden和Survivor的大小比例是8：1，也就是每次新生代可用内存
空间为整个新生代容量的90%，只有10%会被浪费。当然，我们没有办法保证每次回收只有不多于
10%的对象存活，98%的对象可回收只是一般情况，当Survivor空间不够用时，需要依赖其他内存
（这里指老年代）进行分配担保。关于该算法的执行过程，后面会详细讲解。

### 3.3.3 标记-整理算法
&emsp;&emsp;复制收集算法在对象存活率较高的时候要进行较多的复制操作，效率将会变低。更关键
的是如果不想浪费50%的空间，就需要有额外的空间进行分配担保，以应对被使用的内存中所有对象都
存活的极端情况，所以在老年代一般不能直接选用这种算法。

&emsp;&emsp;根据老年代的特点（存活率高），有人提出了标记-整理算法，标记过程与“标记-清除”
算法一样，但后续步骤不是直接对可回收对象进行清理，而是让所有存活的对象都向一端移动，然后
直接清理掉端边界以外的内存，“标记-整理”算法的示意图如图3-4所示：

 ![Image text](https://github.com/hhtqaq/jvmnotes/raw/master/3_gc_and_memory_allocation/img/mark-arrange.png)

### 3.3.4 分代收集算法
&emsp;&emsp;当前商业虚拟机的垃圾收集算法都采用“分代收集”算法，这种算法并没有什么新的思想，
只是根据**对象的存活周期不同**而将内存划分为几块。一般是把Java堆分为新生代和老年代，再根据
各个年代的特点采用最适当的收集算法。新生代中，每次垃圾收集时都发现有大批对象死去，只有**少量
存活**，那就选用复制算法，只需要付出少量存活对象的复制成本就可以完成收集。而老年代中因为对象
存活率高、没有额外的空间对它进行分配担保,就必须使用“标记-清理”或者“标记-整理”算法来进行
回收。

## 3.4 HotSpot算法实现

### 3.4.1 枚举根节点
&emsp;&emsp;从可达性分析中从GC Roots 节点找引用链这个操作为例，可作为GC Roots的节点主要在全局
性的引用（例如常量或类静态属性）与执行上下文（例如栈帧中的本地变量表）中，现在很多应用仅仅方法
区就有数百兆，如果要逐个检查这里面的引用，那么必然会**消耗很多时间**。

&emsp;&emsp;另外，可达性分析对执行时间的敏感还体现在GC停顿上，因为这项分析工作**必须在整个分析
期间整个执行系统看起来就像被冻结在某个时间点上，不可以出现分析过程中对象引用关系还在不断变化
的情况**，否则准确性无法得到保证（就好像在羊圈里数羊，还一直在增加羊）。这点是导致GC进行时必须
停顿所有Java执行线程（Stop The World）的其中一个重要原因，即使是在号称几乎不会发生停顿的CMS
收集器中，枚举根节点也是必须要停顿的。

&emsp;&emsp;对于一个十分复杂的系统，每次GC的时候都要遍历所有的引用肯定是不现实的。为了让GC的
时候快一点，以便让程序高效的完成工作，最早由保守式GC和后来的准确式GC（目前主流的jvm都采用这种）。

1. 保守式GC（主流虚拟机不使用，这里不解释）
2. 与保守式GC相对的就是准确式GC，何为准确式GC？就是我们准确的知道，对于某个位置上的数据是什么
类型的，这样就可以判断出所有位置上的数据是不是指向GC堆的引用，包括栈和寄存器里的数据。

&emsp;&emsp;在java中实现的方式是：从外部记录下类型信息，存成映射表，在HotSpot中把这种映射表称之
为OopMap,不同的虚拟机名称可能不一样。在类加载完成的时候，HotSpot就把对象内什么偏移量上是什么
类型的数据计算出来。

&emsp;&emsp;实现这种功能，需要虚拟机的解释器和JIT编译器支持，由他们来生成OopMap。生成这样的映射
表一般有两种方式：
 - 每次都遍历原始的映射表，循环的一个偏移量扫描过去；这种用法也叫“解释式”
 - 为每个映射表生成一块定制的扫描代码，以后每次要用映射表就直接执行生成的扫描代码；这种用法也叫
 “编译式”
 
&emsp;&emsp;总而言之，GC开始的时候，就通过OopMap这样的一个映射表知道，在对象内的什么偏移量上
 是什么类型的数据，而且特定的位置记录下栈和寄存器中哪些位置是引用。

### 3.4.2 安全点
&emsp;&emsp;上面讲到了为了快点进行可达性分析，使用了一个引用类型的映射表，可以快速的知道对象内
或者栈和寄存器中哪些位置是引用了。

&emsp;&emsp;但是随之而来的又有一个问题，就是在方法执行的过程中，可能会导致引用关系发生变化（**多态**），
那么保存的OopMap就要随着变化。如果每次引用关系发生了变化都要去修改OopMap的话，这又是一件成本
很高的事情。所以这里就引入了安全点的概念（类似**延迟加载**）。

&emsp;&emsp;什么是安全点？OopMap的作用是为了在GC的时候，快速进行可达性分析，所以OopMap并不需
要一发生改变就去更新这个映射表。只要这个更新在GC发生之前就可以了。所以OopMap只需要在预先选定
的一些位置上记录变化的OopMap就行了。这些特定的点就是SafePoint（安全点）。由此也可以知道，
**程序并不是在任意位置都可以进行GC的，只有在达到这样的安全点才能暂停下来进行GC**。

&emsp;&emsp;既然安全点决定了GC的时机，那么安全点的选择就极为重要了。安全点太少，会让GC等待的
时间太长，太多会浪费性能。所以一般会在如下几个位置选择安全点：
 - 循环的末尾
 - 方法临返回前/调用方法的call指令后
 - 可能抛异常的位置 

&emsp;&emsp;还有一个需要考虑的问题就是，如何让程序在进行GC的时候都跑到最近的安全点停顿下来。
这里有两种方案：

&emsp;&emsp; 1. 抢断式中断

&emsp;&emsp;抢断式中断就是在GC的时候，让所有的线程都中断，如果这些线程中发现中断地方不在安全
点上，就恢复线程，让他们重新跑起来，直到跑到安全点上。（现在机会没有虚拟机采用这种方式）

&emsp;&emsp; 2.主动式中断

&emsp;&emsp;主动式中断在GC的时候，不会主动去中断线程，仅仅是设置一个标识，当程序运行到安全
点时就去轮询该位置，发现该位置被设置为真时就自己中断挂起。所以轮询标志的地方是和安全点重合的，
另外创建对象需要分配的地方也需要轮询该位置。

### 3.4.3 安全区域
&emsp;&emsp;安全点的使用似乎解决了OopMap计算的效率的问题，但是这里还有一个问题。安全点
需要程序自己跑过去，那么对于那些已经停在路边休息或者看风景的程序（比如那些处在Sleep或
者Blocked状态的线程），他们可能并不会在很短的时间内跑到安全点去。所以这里为了解决这个
问题，又引入了安全区域的概念。

&emsp;&emsp;安全区域很好理解，就是在程序的一段代码片段中并不会导致引用关系发生变化，
也就不用去更新OopMap表了，那么在这段代码区域内任何地方进行GC都是没有问题的。这段区域
就称之为安全区域。线程执行的过程中，如果进入到安全区域内，就会标志自己已经进行到安全
区域了。那么虚拟机要进行GC的时候，发现该线程已经运行到安全区域，就不会管该线程的死活
了。所以，该线程在脱离安全区域的时候，要自己检查系统是否已经完成了GC或者根节点枚举
（这个跟GC的算法有关系），如果完成了就继续执行，如果未完成，它就必须等待收到可以安全
离开安全区域的Safe Region的信号为止。

 