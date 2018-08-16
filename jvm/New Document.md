#深入Java虚拟机-读书笔记二
##垃圾收集器

当我们讨论垃圾收集的时候，也就是讨论三个问题<br>
1. 哪些内存需要回收？堆？方法区？方法区里面的常量池？还是其他？  
2. 什么时候回收？内存不够的时候回收还是定时回收？  
3. 如何回收？回收的依据算法是什么？


###哪些内存需要回收？
前面笔记记录Java内存运行时区域时，其中程序计数器，虚拟机栈，本地方法栈3个区域随着线程而生命周期。因此这几个区域的内存分配和回收都具备确定性，随着方法结束或者线程结束时，内存自然就跟着回收了。<br>而Java堆和方法区则不一样，一个接口中的多个实例类需要的内存可能不一样，我们只有在程序运行期间才能知道会创建哪些对象，这部分内存的分配和回收都是动态的。垃圾收集器回收的就是这部分内存
>堆和方法区需要垃圾收集器回收


###如何判断对象是否垃圾对象？
Java堆中存放着几乎所有的对象实例，垃圾回收前，首先要做的就是确定哪些对象是垃圾对象
####引用技术算法
给对象中添加一个引用计数器，每当有一个地方引用它时，计数器值就加1，当引用失效时，计数器值就减1，任何时候计数器为0的对象就是不可能再被使用的。<br>
基于该算法的实现简单，判定效率也很高，在大部分情况下它还是一个不错的算法。但是，目前主流的Java虚拟机里面没有选用改算法，主要原因是它很难解决对象之间相互循环引用的问题。比如对象A里面有个对象B的引用，而B对象里面也有个A对象的引用
####可达性分析算法
可达性分析算法的思路就是通过一系列称为"GC Roots"的对象为起始点，从这些节点开始往下搜索，搜索走过的路径称为引用链，当一个对象到GC Roots没有任何引用链连接时，则证明此对象是垃圾对象.
<br>如下图 objecet 5 object6 object 7虽然互相有关联，但是没有在GC Roots引用链中，所以它们都被认为是可收回的垃圾对象
![avatar](/4.png)

那什么对象又可被称为GC Roots对象呢？<br>
Java中可作为GC Roots如下几种:<br>
1. 虚拟机栈中应用的对象  
2. 方法区中类静态属性引用的对象(static修饰的引用对象)  
3. 方法区中常量引用的对象(final修饰引用对象)  
4. 本地方法栈中JNI引用的对象(native方法应用的对象)  

####引用
Java中应用分为4种，强引用，软引用，弱引用，虚引用。  

**强引用** .
>平常我们的代码 类似"Object o=new Object()"，这类的引用，只要强引用还存在，垃圾收集器就永远不会回收掉用引用的对象

**软引用**
>描述一些还有用但并非必需的对象。在系统将要发生内存溢出异常之前，将会把这些对象列进回收范围之中进行第二次回收。SoftReference类来实现软引用

**弱引用**
>也是描述一些非必需的对象。每当垃圾回收器回收时，都会回收被弱引用关联的对象。WeakReference类来实现弱引用     

**虚引用**  
>称为幽灵引用或者幻影引用，它是最弱的一种引用关系。一个对象是否有虚引用的存在，完全不会对其生存时间构成影响。为一个对象设置虚引用关联的唯一目的就是能在这个对象被回收器回收是收到一个系统通知。PhantomReference类来实现弱引用

生存还是死亡，这是个问题。即使在可达性分析算法中不可达的而对象，也并非立即进行回收，这个时候它们暂时处于"缓刑"阶段，要真正判断一个对象已经死亡，至少要经历两次标记过程：如果对象在进行可达性分析后发现是垃圾对象，那它会被第一次标记并且进行一次筛选，筛选的条件是此对象是否有必要执行finalize()方法。当对象没有覆盖finalize()方法，或者finalize()方法已经被虚拟机调用过，这个时候才会真正回收。

	public class Test3 {
		static B b;
		static class B{
		public void println() {
				System.out.println("我还存活着....");
			}
			@Override
			protected void finalize() throws Throwable {
				super.finalize();
				System.out.println("finalize方法执行了....");
				//自我拯救
				Test3.b=this;
			}
		}
		public static void main(String[] args) throws Exception {
			b=new B();
			b=null;
			System.gc();
			Thread.sleep(500);
			if(b==null) {
				System.out.println("b已经被回收了....");
			}else {
				b.println();
			}
			b=null;
			System.gc();
			Thread.sleep(500);
			if(b==null) {
				System.out.println("b已经被回收了....");
			}else {
				b.println();
			}
			
		}
	}

输出日志，可以看到finalize()方法是对象逃脱死亡的最后一个机会。并且finalize()方法都只会被系统自动调用一次。
>finalize方法执行了....  
我还存活着....  
b已经被回收了....  


##垃圾收集算法
###标记-清除算法
这是最基础的算法：首先标记出所有需要回收的对象，在标记完成后统一回收所有被标记的对象

该算法主要有两个不足:
>一个是效率问题，标记和清除两个过程的效率都不高  
>一个是空间问题，清除后会产生大量不连续的内存碎片
![avatar](/5.png)

###复制算法
为了解决效率问题，"复制"算法出现，它将可用内存按容量划分为大小相等的两块，每次只使用其中的一块。当这一块的内存用完了，将还存活的对象复制到另外一块上面，然后再把已经使用过的内存空间一次清理掉。<br>
>这种算法实现简单，运行高效，但是代价是将内存缩小了原来的一半。
![avatar](/6.png)

###标记-整理算法
标记过程与标记-清除算法一样，但后续不是直接对可回收对象进行清理，而是让所有存活的对象都向一端移动，然后直接清理掉端边界以外的内存。
![avatar](/7.png)

###分代收集算法
根据对象存活周期的不容将内存划分为几块。一般是把Java堆分为新生代和老年代，这个可以根据各个年代的特点采用最适当的手机算法。在新生代，每次垃圾回收时都会发现有大批对象死去，只有少量存活，就选用复制算法。而老年代对象存活率高，则使用标记-整理或者标记-清理算法回收