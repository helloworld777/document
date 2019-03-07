
#Android系统启动流程之SystemServer进程启动

Android系统中各个进程的先后顺序为：  
**init进程 –-> Zygote进程 –> SystemServer进程 –>应用进程**

其中Zygote进程由init进程启动，SystemServer进程和应用进程由Zygote进程启动。

###SystemServer进程创建

SystemServer进程主要是用于创建系统服务的，例如AMS、WMS、PMS。

SystemService进程被创建后，主要的处理如下：

   >1、启动Binder线程池，这样就可以与其他进程进行Binder跨进程通信。  
    2、创建SystemServiceManager，它用来对系统服务进行创建、启动和生命周期管理。  
    3、启动各种系统服务：引导服务、核心服务、其他服务，共100多种。应用开发主要关注引导服务ActivityManagerService、PackageManagerService和其他服务WindowManagerService、InputManagerService即可。


**系统服务分成了三种类型：引导服务、核心服务、其它服务。这些系统服务共有100多个，其中对于我们来说比较关键的有：**

   >引导服务：ActivityManagerService，负责四大组件的启动、切换、调度。  
    引导服务：PackageManagerService，负责对APK进行安装、解析、删除、卸载等操作。  
    引导服务：PowerManagerService，负责计算系统中与Power相关的计算，然后决定系统该如何反应。  
    核心服务：BatteryService，管理电池相关的服务。  
    其它服务：WindowManagerService，窗口管理服务。  
    其它服务：InputManagerService，管理输入事件。  



###系统服务的创建

很多系统服务的启动逻辑都是类似的，以启动ActivityManagerService服务来进行举例，代码如下所示：

	mActivityManagerService = mSystemServiceManager.startService(
        ActivityManagerService.Lifecycle.class).getService();

`ActivityManagerService`的方法 `startService`

	@SuppressWarnings("unchecked")
	public <T extends SystemService> T startService(Class<T> serviceClass) {
	    		...
	            Constructor<T> constructor = serviceClass.getConstructor(Context.class);
	            // 1
	            service = constructor.newInstance(mContext);
	        // 2
	        startService(service);
	        return service;
	   		...
	}
	public void startService(@NonNull final SystemService service) {
	    // 1
	    mServices.add(service);
	    // Start it.
	    long time = SystemClock.elapsedRealtime();
	        // 2
	     service.onStart();
	   ...;
	}

在注释1处，首先会将ActivityManagerService添加在mServices中，它是一个存储SystemService类型的ArrayList，这样就完成了ActivityManagerService的注册。在注释2处，调用了ActivityManagerService的onStart()方法完成了启动ActivityManagerService服务。

除了使用SystemServiceManager的startService()方法来启动系统服务外，也可以直接调用服务的main()方法来启动系统服务，如PackageManagerService：

	mPackageManagerService = PackageManagerService.main(mSystemContext, installer,
        mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);


ServiceManager用于管理系统中的各种Service，用于系统C/S架构中的Binder进程间通信，即如果Client端需要使用某个Servcie，首先应该到ServiceManager查询Service的相关信息，然后使用这些信息和该Service所在的Server进程建立通信通道，这样Client端就可以服务端进程的Service进行通信了。