##一个枚举搞定日志日志

平常我们打印方法的参数和返回值时，是在方法里面调用Log.d直接打印参数和返回值。基本所有的逻辑都是在方法第一行代码打印方法的参数，在方法的最后一行打印返回值。  
写的多了就想。为啥不能所有要打印的方法统一处理呢？如果在方法前面加入注释，就能处理这些，那代码岂不写的很酸爽？   
回顾以往的知识，在不改变原有方法代码的前提下，动态添加代码，首先想到的是动态代理，动态代理确实可以拿到代理的方法参数，并且打印，但是返回值无法获取，并且动态代理有个很大的不足，就是代理的对象一定是要实现了某个接口。  
动态代理既然行不通，那又如果动态添加代码？  
联想到以前的web知识的时候，当时实现AOP的框架aspectj不正是可以动态在方法运行前，和运行后添加代码  

而现在aspectj早已支持在android上。于是发现前面的想法不是马上就可以落地生根了，开始撸起demo来  

我把它单独放入一`个librar`y依赖里面,下面是依赖库的`build.gradle`

	apply plugin: 'com.android.library'
	import org.aspectj.bridge.IMessage
	import org.aspectj.bridge.MessageHandler
	import org.aspectj.tools.ajc.Main
	
	buildscript {
	    repositories {
	        mavenCentral()
	    }
	    dependencies {
	        classpath 'com.android.tools.build:gradle:2.1.0'
	        classpath 'org.aspectj:aspectjtools:1.8.9'
	        classpath 'org.aspectj:aspectjweaver:1.8.9'
	    }
	}
	
	android {
	    compileSdkVersion 28
	
	
	
	    defaultConfig {
	        minSdkVersion 15
	        targetSdkVersion 28
	        versionCode 1
	        versionName "1.0"
	
	        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
	
	    }
	
	    buildTypes {
	        release {
	            minifyEnabled false
	            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
	        }
	    }
	
	}
	
	android.libraryVariants.all { variant ->
	    JavaCompile javaCompile = variant.javaCompile
	    javaCompile.doLast {
	        String[] args = [
	                "-showWeaveInfo",
	                "-1.5",
	                "-inpath", javaCompile.destinationDir.toString(),
	                "-aspectpath", javaCompile.classpath.asPath,
	                "-d", javaCompile.destinationDir.toString(),
	                "-classpath", javaCompile.classpath.asPath,
	                "-bootclasspath", android.bootClasspath.join(File.pathSeparator)
	        ]
	
	        MessageHandler handler = new MessageHandler(true);
	        new Main().run(args, handler)
	
	        def log = project.logger
	        for (IMessage message : handler.getMessages(null, true)) {
	            switch (message.getKind()) {
	                case IMessage.ABORT:
	                case IMessage.ERROR:
	                case IMessage.FAIL:
	                    log.error message.message, message.thrown
	                    break;
	                case IMessage.WARNING:
	                case IMessage.INFO:
	                    log.info message.message, message.thrown
	                    break;
	                case IMessage.DEBUG:
	                    log.debug message.message, message.thrown
	                    break;
	            }
	        }
	    }
	}
	dependencies {
	    implementation fileTree(dir: 'libs', include: ['*.jar'])
	    compile 'org.aspectj:aspectjrt:1.8.9'
	    compile 'com.android.support:appcompat-v7:25.4.0'
	    testImplementation 'junit:junit:4.12'
	    androidTestImplementation 'com.android.support.test:runner:1.0.2'
	    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
	}

编写一个枚举，用于标识哪些方法需要打印参数和返回值。

	@Retention(RetentionPolicy.CLASS)
	@Target({ ElementType.CONSTRUCTOR, ElementType.METHOD })
	public @interface DebugTrace {}

写一个类，用Aspect注解，用于处理DebugTrace标记的方法。


	@Aspect
	public class DebugTraceTraceAspect {
	
	  private static final String POINTCUT_METHOD =
	      "execution(@com.lu.aoplib.annotation.DebugTrace * *(..))";
	
	  private static final String POINTCUT_CONSTRUCTOR =
	      "execution(@com.lu.aoplib.annotation.DebugTrace *.new(..))";
		
       //要处理用DebugTrace标识的方法
	  @Pointcut(POINTCUT_METHOD)
	  public void methodAnnotatedWithDebugTrace() {}
	 //要处理用DebugTrace标识的构造器
	  @Pointcut(POINTCUT_CONSTRUCTOR)
	  public void constructorAnnotatedDebugTrace() {}
	
	 //真正处理的地方
	  @Around("methodAnnotatedWithDebugTrace() || constructorAnnotatedDebugTrace()")
	  public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
	
		//拿到DebugTrace标识的Signature对象，该对象可以获取到DebugTrace标识的类名和方法名，
	    Signature signature=joinPoint.getSignature();
		//获取类名
	    String className = signature.getDeclaringType().getSimpleName();
		//获取方法名
	    String methodName = signature.getName();
		//在方法名插入时间戳
	    final StopWatch stopWatch = new StopWatch();
	    stopWatch.start();
		//执行方法，并拿到返回值
	    Object result = joinPoint.proceed();
		//获取方法后执行的时间戳，然后可以计算方法耗时
	    stopWatch.stop();
	    StringBuilder stringBuilder=new StringBuilder();

		//拿到方法的参数
	    Object[] args=joinPoint.getArgs();
		//拼接参数
	    if (args!=null&&args.length>0){
	      stringBuilder.append("[ param:");
	      for (Object a:args){
	        stringBuilder.append(a);
	        stringBuilder.append(",");
	      }
	
	    }
		//如果方法是void类型，则没有返回值
	    if (result instanceof Void){
	
	    }else{
	      stringBuilder.append("-->result:"+result);
	    }
	    stringBuilder.append("]");
		//拼接时间
	    stringBuilder.append("["+stopWatch.getTotalTimeMillis()+"ms]");
	    Log.d(getClass().getSimpleName(),stringBuilder.toString());
		//打印日志
	    LogUtil.d2(stringBuilder.toString());
	    return result;
	  }
Aspect枚举的类告诉Aspect编译器需要处理哪些类哪些方法


LogUtil类 

	public class LogUtil {
	    private static String className;
	    private static String methodName;
	    private static int lineNumber;
	    private static final int JSON_INDENT = 4;
	
	  
	    public static void d(String message) {
	        getMethodNames(new Throwable().getStackTrace(),1);
	        Log.i(className, createLog(message));
	    }
	   
	    public static void d2(String message) {
	        getMethodNames(new Throwable().getStackTrace(),2);
	        Log.i(className, createLog(message));
	    }
	    public static void w(String message) {
	        getMethodNames(new Throwable().getStackTrace(),1);
	        Log.w(className, createLog(message));
	    }
	    public static void d(Object o) {
	        String message=null;
	       if (o==null){
	           message="message is null";
	       }else{
	           message=o.toString();
	       }
	        getMethodNames(new Throwable().getStackTrace(),1);
	        Log.i(className, createLog(message));
	    }
	    private static String createLog(String log) {
	        StringBuffer buffer = new StringBuffer();
	        buffer.append("[");
	        buffer.append("(");
	        buffer.append(className);
	        buffer.append(":");
	        buffer.append(lineNumber);
	        buffer.append(")");
	        buffer.append("#");
	        buffer.append(methodName);
	        buffer.append("]");
	        buffer.append(printIfJson(log));
	        return buffer.toString();
	    }
	    public static String printIfJson(String msg) {
	
	        String message;
	        StringBuffer stringBuffer=new StringBuffer();
	        try {
	            int index=-1;
	            if(msg.contains("{")){
	                index=msg.indexOf("{");
	            }else if (msg.contains("[")){
	                index=msg.indexOf("{");
	            }
	
	            if (index!=-1){
	                stringBuffer.append(msg.substring(0,index));
	                msg=msg.substring(index);
	            }
	
	            if (msg.startsWith("{")) {
	                JSONObject jsonObject = new JSONObject(msg);
	                message = jsonObject.toString(JSON_INDENT);
	            } else if (msg.startsWith("[")) {
	                JSONArray jsonArray = new JSONArray(msg);
	                message = jsonArray.toString(JSON_INDENT);
	            } else {
	                message = msg;
	            }
	        } catch (Exception e) {
	            message = msg;
	        }
	        stringBuffer.append(message);
	        return stringBuffer.toString();
	    }
	    private static void getMethodNames(StackTraceElement[] sElements, int index) {
	        className = sElements[index].getFileName();
	        methodName = sElements[index].getMethodName();
	        lineNumber = sElements[index].getLineNumber();
	    }

	}

统计时长的类

	public class StopWatch {
	  private long startTime;
	  private long endTime;
	  private long elapsedTime;
	  public static int Accuracy = 1; // 1：毫秒   2：微秒
	  public StopWatch() {
	    //empty
	  }
	
	  private void reset() {
	    startTime = 0;
	    endTime = 0;
	    elapsedTime = 0;
	  }
	
	  public void start() {
	    reset();
	    startTime = System.nanoTime();
	  }
	
	  public void stop() {
	    if (startTime != 0) {
	      endTime = System.nanoTime();
	      elapsedTime = endTime - startTime;
	    } else {
	      reset();
	    }
	  }
	  public long getTotalTime(int type){
	    if (type == 1){
	      return getTotalTimeMillis();
	    }else{
	      return getTotalTimeMicros();
	    }
	  }
	  public long getTotalTimeMicros() {
	    return (elapsedTime != 0) ? (long) ((endTime - startTime) / 1000) : 0;
	  }
	  public long getTotalTimeMillis(){
	    return (elapsedTime != 0) ? (long) (TimeUnit.NANOSECONDS.toMicros(endTime - startTime) / 1000) : 0;
	  }
	}

测试的类 

	public class AopTest {
	
	
	    @DebugTrace
	    public String testAop(int a,int b) {
	        try {
	            Thread.sleep(100);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	        return String.valueOf(a+b);
	    }
	}

打印的结果
![](https://i.imgur.com/hTQ2sdh.png)

非常符合预期结果。不仅可以统计打印日志，哪些方法耗时也可以一览无余。  
一个枚举搞定日志打印，以后又有装逼的技巧了。