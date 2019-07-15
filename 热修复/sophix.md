各大修复方案对比

![](https://i.imgur.com/DYjFpUz.png)

代码修复两大主流方案，一种底层替换方案，一种类加载方法  

>底层替换方案限制多，但时效性最好，加载轻快，立即见效   
>类加载方案需要冷启动才能见效，但修复范围广，限制少

###代码修复

**底层替换方案原理：在已经加载了的类中直接替换掉原有方法，是在原来类的基础上进行修改。无法实现对于原有类进行方法和字段的增减。**  

**类加载方案：在APP重新启动后让Classloader去加载新的类。要重启是因为在app运行到一半的时候，所有需要发生变更的类已经被加载过了，而又不能无法对一个类进行卸载。如果不重启，原有的类还在虚拟机中，无法加载新类。而重启的话，则在还加载那个有bug的类之前抢先加载补丁中的新类，这样就不会加载那个有bug的类。**

sophix结合了两种方案：小修改的，在底层替换方案限制范围内的，直接采用底层替换修复，对于代码修改超出底层替换限制的，使用类加载方案。

###资源修复

InstantRun中资源修复分两步：  
1.构造一个新的AssetManager,并通过反射调用addAssetPath,把完整的新资源包加入到这个AssetManager中。  
2.找到之前引用到原AssetManager的地方，通过反射，把引用替换为新的AssetManager

sophix方案：构造了一个package id 为0x66的资源包，这个包只包含改变了的资源项，然后直接在原有的AssetManager中addAssetPath这个包就可以。

###so库修复

把补丁so库的路径插入到nativeLibraryDirectories数组的最前面，就能够达到加载so库的时候是补丁的so库，而不是原来的so库。代码修复这个和类加载的方案有点类似。  
为此sophix需要再启动期间反射注入patch的so库。




###如何兼容底层替换方案?
底层替换方案原理：在已经加载了的类中直接在native层替换掉原有方法。每一个Java方法在art中都对应着一个ArtMethod,ArtMethod记录了这个Java方法的所有信息，包括所属类，访问权限，代码执行地址等.通过env->FromReflectedMehod，可以由Mehod对象得到这个方法对应的ArtMehod的真正起始地址。然后就可以把它强转为ArtMehod指针

Andfix的替换  

	smeth->declaring_class_ = dmeth->dclearing_class_;
	smeth->dex_cache_resolved_methods_ = dmeth->dex_cache_resolved_methods_;
	smeth->dex_cache_resolved_types_ = dmeth-> dex_cache_resolved_types_
	...


![](https://i.imgur.com/vaATwOO.png)

但是由于手机厂商修改了底层的ArtMehod结构体，则就无法正常替换

因此 sophix整体替换

	memcpy(smeth,dmeth,sizeof(ArtMethod));

![](https://i.imgur.com/E9E0NSI.png)


至于如果计算sizeof大小。详细可看原文章。



###内部类编译
外部类为了访问内部类私有的属性/方法，编译期间自动会为内部类生成access&**相关方法

	public class Test {
		public static void main(String[] args) {
			InnerClass inner = new Test().new InnerClass();
			System.out.println(inner.s);;
			inner.hello();
		}
		public  void println() {
			InnerClass inner = new InnerClass();
			System.out.println(inner.s);
		}
		class InnerClass{
			private String s="hehe";
			private void hello() {
				System.out.println("hello");
			}
		}
	}
用javap -c Test得到Test编译后的字节码信息

	public static void main(java.lang.String[]);
	    Code:
	       0: new           #16                 // class com/test/mp3/Test$InnerClass
	       3: dup           
	       4: new           #1                  // class com/test/mp3/Test
	       7: dup           
	       8: invokespecial #18                 // Method "<init>":()V
	      11: dup           
	      12: invokevirtual #19                 // Method java/lang/Object.getClass:()Ljava/lang/Class;
	      15: pop           
	      16: invokespecial #23                 // Method com/test/mp3/Test$InnerClass."<init>":(Lcom/test/mp3/Test;)V
	      19: astore_1      
	      20: getstatic     #26                 // Field java/lang/System.out:Ljava/io/PrintStream;
	      23: aload_1       
	      24: invokestatic  #32                 // Method com/test/mp3/Test$InnerClass.access$0:(Lcom/test/mp3/Test$InnerClass;)Ljava/lang/String;
	      27: invokevirtual #36                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
	      30: aload_1       
	      31: invokestatic  #42                 // Method com/test/mp3/Test$InnerClass.access$1:(Lcom/test/mp3/Test$InnerClass;)V
	      34: return        
  
24行调用了InnerClass的access$0方法，正是上面java里面的inner.s。  
31行调用access$1正是inner.hello()。

查看InnerClass的编译后的信息  
用javap -c Test$InnerClass得到编译后的字节码信息

	Compiled from "Test.java"
	class com.test.mp3.Test$InnerClass {
	  final com.test.mp3.Test this$0;
	
	  com.test.mp3.Test$InnerClass(com.test.mp3.Test);
	    Code:
	       0: aload_0       
	       1: aload_1       
	       2: putfield      #12                 // Field this$0:Lcom/test/mp3/Test;
	       5: aload_0       
	       6: invokespecial #14                 // Method java/lang/Object."<init>":()V
	       9: aload_0       
	      10: ldc           #17                 // String hehe
	      12: putfield      #19                 // Field s:Ljava/lang/String;
	      15: return        
	  
	  static java.lang.String access$0(com.test.mp3.Test$InnerClass);
	    Code:
	       0: aload_0       
	       1: getfield      #19                 // Field s:Ljava/lang/String;
	       4: areturn       
	
	  static void access$1(com.test.mp3.Test$InnerClass);
	    Code:
	       0: aload_0       
	       1: invokespecial #43                 // Method hello:()V
	       4: return        
	}

编译器自动给我们的s属性添加了static方法 access$0;并且也生成了access$1,里面调用了hello方法。


