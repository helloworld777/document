###配置Gradle环境

前提:配置好Java环境<br>
把gradle 对象的bin目录配置到系统环境变量里面


![avatar](/20180716104816.png)

出现如下图，则表示环境已经配置好了


![avatar](/2.png)

###Gradle版 hello world
创建一个build.gradle文件

添加gradle版的hello world脚本代码<br>

	task hello{
		doLast{
			println 'hello world'
		}
	}


在控制台输入如下命令，编译完成后会出现hello world字符，正是我们println输出的
![avatar](/3.png)

build.gradle是Gradle默认的构造脚本文件，执行gradle命令的时候，会默认加载当前目录下的build.gradle脚本文件<br>
这个构建脚本定义一个任务，这个任务名字叫hello,并且给任务hello添加了一个Action,它其实就是一段Groovy语言实现的闭包。doLast就意味着在Tast执行完毕后要回调doLast的这部分闭包的代码<br>
再看gradle -q hello这个命令，意思是要执行build.gradle脚本中定义的名为hello的Task,-q 参数用于控制gradle输出的日志级别，以及哪些日志可以输出被看到。  
如果不加-q命令，如下  

![avatar](/4.png)

是不是有点类型我们的android studio里面的EventLog里面的日志  
看到println 'hello world',它会输出hello world,通过名字大家已经猜出来，它其实就是System.out.println("hello world")的简写方法,Gradle可以识别它，是因为Groovy已经把println（）这个方法添加到java.lang.Object，而在Groovy中，方法的调用可以省略签名中的括号，以一个空格分开即可，所以就有了上面的写法。说明的是，在Groovy中，单引号和双引号所包含的内容都是字符串  


###Gradle 日志

前面我们通过-q参数控制命令，Gradle的日志级别还有其他类型，如下

![avatar](/5.png)

要使用它们，则通过命令行参数开关控制

![avatar](/6.png)

输出错误堆栈信息 ,默认情况堆栈信息的输出是关闭的，我们可以同如下参数打开它。这样在我们构建失败的时候，Gradle才会输出错误堆栈信息

![avatar](/7.png)

使用自己的日志调试，如果我们需要输出一些日志，通常情况下我们使用print系列方法，把日志信息输出到标准的控制台输出流

printl '输出一个日志信息'

除了print系列方法，也可以使用内置的logger更灵活的控制输出不同级别的日志信息：
>logger.quiet('message')  
>logger.error('message')    
>logger.warn('message')    
>logger.lifecycle('message')    
>logger.info('message')    
>logger.debug('message')    

这里其实是调用Project的getLogger()方法阿获取Logger对象的实例

##Groovy 基础

 Groovy是基于JVM虚拟机的一种动态语言，语法和Java相似，完全兼容Java，又在此基础上增加了很多动态类型和灵活的特性，比如支持闭包，支持DSL，可以说它是一门非常灵活的动态脚本语言  
每个Gradle的build脚本文件都是一个Groovy脚本文件，你可以在里面写任何符号Groovy语法的代码，而Groovy又完全兼容Java,所有你也可以在build脚本文件里面写任何Java代码


###字符串

添加代码,

	task printlnStr <<{
	def str1='单引号'
	def str2='双引号'
	
	println '单引号类型：'+str1.getClass().name
	println '双引号类型：'+str2.getClass().name
	
	def name="张三"
	println '单引号变量：${name}'
	println "双引号变量：${name}"
	}

输出日志，可以看到单引号和双引号都是String类型，但是单引号不能对字符串里面的表达式做运算，双引号可以直接进行表达式计算，一个美元符号紧跟一对花括号，花括号里面放表达式，如何${name},${1+1}等，只有一个变量的时候可以省略花括号，如$name

![avatar](/8.png)

通常我们在app 的build.gradle 也常常看到，如下，我们就知道SUPPORTVERSION就是一个变量，并且这里不能用单引号表示

	implementation "com.android.support:appcompat-v7:$SUPPORTVERSION"
    implementation "com.android.support:cardview-v7:$SUPPORTVERSION"

###集合

Groovy完全兼容了Java的集合，并且进行了扩展  

###List
定义一个List,访问List集合

	task printList <<{
		def numList=[1,2,3,4,5];
		println numList.getClass().name
		
		println numList[1] //访问第二个元素
		println numList[-1] //访问最后一个元素
		println numList[1..3] //访问第二个元素到第四个元素
		numList.each{
				println it
			}
	}

输出，可以看到numList是一个ArrayList类型，访问方式也多种，其中each方法可以方便的迭代操作，该方法接收一个闭包作为参数，可以访问List里面的每个元素

![avatar](/9.png)


定义一个Map,访问Map集合

	task printMap <<{
		def map=['w':123,'h':456];
		println map.getClass().name
		
		println map['w'] 
		println map.h 
		
		map.each{
			println "key:${it.key},value:${it.value}"
		}
	}

![avatar](/10.png)

###方法
这个是重点了  
>在Java里面调用方法是method(parm1,parm2),而在Groovy里面，可以省略(),变成method parm1,parm2

代码    

	task testMethod <<{
		add(1,2)
		add 1,2
	}
	def add(int a,int b){
		println a+b 
	}
输出
![avatar](/11.png)

>**return可以不写** ，Groovy中，定义有返回值的方法时，return 语句不是必需的，当没有return时候，最后一句代码作为返回值

代码  

	task testMethod <<{
		add(1,2)
		add 1,2
		def result=method2 1,2
		println "result:$result";
	}
	def add(int a,int b){
		println a+b 
	}
	
	def method2(int a,int b){
		a+b
	}

输出
![avatar](/12.png)


>代码块可以作为参数传递，代码块--一段被花括号包围的代码，也就是闭包。Groovy允许其作为参数传递


以上面集合each方法为例子,调用集合的each方法，传入代码块
 	
	numList.each({
		println it
	})
	//Groovy规定，如果方法的最后一个参数是闭包，可以方法方法外面
	numList.each(){
		println it
	}
	//由于方法参数括号可以省略，因此变成这样的样式
	numList.each{
		println it
	}


###闭包
前面说过闭包就是一段代码块，下面我们先自己写一个闭包。我们定义了一个方法customEach，它只有一个参数用于接收一个闭包，那么闭包怎么执行呢？调用方法的时候后面跟一对括号就是执行了。
括号里的参数就是该闭包接收的参数，
>如果参数只有一个，那么就是it变量

	task testClosure <<{
		customEach{
			println it
		}
	}
	def customEach(closure){
		for(int i in i..10){
			closure(i)
		}
	}

输出
![avatar](/13.png)

>多个参数，当闭包只有一个参数是，默认就是it,当有多个参数时，就需要把参数一一列出

	task testClosure2 <<{
		eachMap{
			k,v -> println "${k} is ${v}"
		}
	}
	def eachMap(closure){
		def map=['w':123,'h':456];
		map.each{
			closure(it.key,it.value)
		}
	}

![avatar](/14.png)

像我们平常要更改生成APP的名称，在后面添加版本号。会在build.gradle添加如下代码，现在我们知道执行的是applicationVariants的all方法，后面是一个闭包，闭包里面有个variant变量，而variant的outputs.add方法也是有一个闭包，执行的就是我们修改输出app的apk包名

	//在apk文件后边生成版本号信息
    applicationVariants.all {
        variant ->
            variant.outputs.add {
                outputFileName = new File(output.outputFile.parent, "app_" + "V" + defaultConfig.versionName + ".apk");
            }
    }

>闭包委托

Groovy闭包的强大之处在于它之处闭包方法的委托，闭包有thisObject,owner,delegate三个属性，当你再闭包内调用方法时，由它来确定使用哪个对象来处理。默认情况下deletgate和owner是相等的，但是delegate是可以被修改。

示例代码，定义了一个方法person。设置了委托对象为当前创建的Person实例，并且设置了委托模式优先，所以，我们在使用person方法创建一个Person的实例时，可以在闭包里直接对该Person实例配置。

	task configClosure <<{
	
		person{
			name="张三"
			age=18
			dumpPerson()
		}
	
	}
	class Person{
		String name
		int age
		def dumpPerson(){
			println "name is ${name},age is ${age}"
		}
		
	}
	def person(Closure<Person> closure){
		Person p=new Person()
		closure.delegate=p
		//委托模式优先
		closure.setResolveStrategy(Closure.DELEGATE_FIRST)
		closure(p)
	}


![avatar](/16.png)