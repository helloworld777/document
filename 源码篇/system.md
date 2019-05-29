
###zygote进程
zygote 服务器从app_process启动后，会启动一个虚拟机。虚拟机执行第一个Java类是ZygoteInit.java.  

ZygoteInit的main方法

 	public static void main(String argv[]) {
        ZygoteServer zygoteServer = new ZygoteServer();

        // Zygote goes into its own process group.
        try {
            Os.setpgid(0, 0);
        } catch (ErrnoException ex) {
            throw new RuntimeException("Failed to setpgid(0,0)", ex);
        }

        final Runnable caller;
        try {
 		      ...
			//1 创建一个ServerSocket
            zygoteServer.registerServerSocket(socketName);
            // In some configurations, we avoid preloading resources and classes eagerly.
            // In such cases, we will preload things prior to our first fork.
			// 2 预加载类和资源
            if (!enableLazyPreload) {
                bootTimingsTraceLog.traceBegin("ZygotePreload");
                EventLog.writeEvent(LOG_BOOT_PROGRESS_PRELOAD_START,
                    SystemClock.uptimeMillis());
                preload(bootTimingsTraceLog);
                EventLog.writeEvent(LOG_BOOT_PROGRESS_PRELOAD_END,
                    SystemClock.uptimeMillis());
                bootTimingsTraceLog.traceEnd(); // ZygotePreload
            } else {
                Zygote.resetNicePriority();
            }

           ...
			
            if (startSystemServer) {
				//3 创建SystemServer进程
                Runnable r = forkSystemServer(abiList, socketName, zygoteServer);

                // {@code r == null} in the parent (zygote) process, and {@code r != null} in the
                // child (system_server) process.
                if (r != null) {
                    r.run();
                    return;
                }
            }

            Log.i(TAG, "Accepting command socket connections");

            // The select loop returns early in the child process after a fork and
            // loops forever in the zygote.
			//如果是zygote进程，则运行该方法，一直阻塞。等待客户端消息，如果是其他进程则在上面return了。
			//4 
            caller = zygoteServer.runSelectLoop(abiList);
        } catch (Throwable ex) {
            Log.e(TAG, "System zygote died with exception", ex);
            throw ex;
        } finally {
			//5 关闭其他进程的socket端口
            zygoteServer.closeServerSocket();
        }

        // We're in the child process and have exited the select loop. Proceed to execute the
        // command.
        if (caller != null) {
            caller.run();
        }
    }

>1 ZygoteInit main函数第一个重要工作就是启动一个Socket服务器端口  
>2 创建完Socket后，预装Framework大部分类及资源  
>3 加载完毕后，创建SystemServer进程  
> 前面注释3的地方，创建新进程，此方法会返回两个结果，一个r为null。是当前进程，一个r不为null.是子进程。
> 当r为null时，则执行注释4。runSelectLoop方法里面是个死循环，等待客户端发送消息。
> 当r不为null时，则是在子进程，因为子进程不需要这个端口。因此关闭


	  private static Runnable forkSystemServer(String abiList, String socketName,
	            ZygoteServer zygoteServer) {
	       ...
	        int pid;
	
	        try {
	            parsedArgs = new ZygoteConnection.Arguments(args);
	            ZygoteConnection.applyDebuggerSystemProperty(parsedArgs);
	            ZygoteConnection.applyInvokeWithSystemProperty(parsedArgs);
	
	            /* Request to fork the system server process */
				//1 创建一个SystemServer进程
	            pid = Zygote.forkSystemServer(
	                    parsedArgs.uid, parsedArgs.gid,
	                    parsedArgs.gids,
	                    parsedArgs.debugFlags,
	                    null,
	                    parsedArgs.permittedCapabilities,
	                    parsedArgs.effectiveCapabilities);
	        } catch (IllegalArgumentException ex) {
	            throw new RuntimeException(ex);
	        }
			
	        /* For child process */
			//2 SystemServer进程执行的代码
	        if (pid == 0) {
	            if (hasSecondZygote(abiList)) {
	                waitForSecondaryZygote(socketName);
	            }
				//3 关闭socket的
	            zygoteServer.closeServerSocket();
	            return handleSystemServerProcess(parsedArgs);
	        }
	
	        return null;
	    }

>注释 1 ，最终会调用nativeForkSystemServer方法创建一个进程  
注释 2 pid == 0 则是创建出来的进程执行的代码。handleSystemServerProcess最终调用RuntimeInit里面的findStaticMain方法。该方的run方法里面执行目标的main方法  
注释 3 关闭服务器端口。因为从zygote复制前一句创建了Socket服务端，这个服务端除了zygote自己，不应该被其他进程使用，否则系统中会有多个进程接收Socket客户端的命令

###SystemServer进程
SystemServer是zygote孵化出的第一个进程，从ZygoteInit main函数调用startSystemServer()开始,最终调用nativeForkSystemServer方法  
启动新进程后，关闭Socket服务器端。执行SystemServer的main方法  

	public static void main(String[] args) {
	        new SystemServer().run();
	  }

 	private void run() {
        try {     
           ...
            // Prepare the main looper thread (this thread).
            android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_FOREGROUND);
            android.os.Process.setCanSelfBackground(false);
			//设置Looper
            Looper.prepareMainLooper();
			
            // Initialize native services.
			//加载so库
            System.loadLibrary("android_servers");

            // Check whether we failed to shut down last time we tried.
            // This call may not return.
            performPendingShutdown();
			
			//创建一个ActivityThread,ContextImpl
            // Initialize the system context.
            createSystemContext();

            // Create the system service manager.
            mSystemServiceManager = new SystemServiceManager(mSystemContext);
            mSystemServiceManager.setRuntimeRestarted(mRuntimeRestart);
            LocalServices.addService(SystemServiceManager.class, mSystemServiceManager);
            // Prepare the thread pool for init tasks that can be parallelized
			//初始化一个包含执行任务的线程池的单例，某些服务在线程池中执行
            SystemServerInitThreadPool.get();
        } finally {
            traceEnd();  // InitBeforeStartServices
        }

        // Start services.
        try {
            traceBeginAndSlog("StartServices");
			//运行一些启动服务,比如电池管理服务，屏幕亮度服务，PMS服务
            startBootstrapServices();
			//运行一些核心服务,
            startCoreServices();
			//运行其他服务器
            startOtherServices();
            SystemServerInitThreadPool.shutdown();
        } catch (Throwable ex) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting system services", ex);
            throw ex;
        } finally {
            traceEnd();
        }

        ...

        // Loop forever.
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
	
启动服务的大部分会调用到SystemServiceManager的startService方法

	  public void startService(@NonNull final SystemService service) {
        // Register it.
        mServices.add(service);
        // Start it.
        long time = SystemClock.elapsedRealtime();
        try {
			//调用 service 里面的onStart方法
            service.onStart();
        } catch (RuntimeException ex) {
            throw new RuntimeException("Failed to start service " + service.getClass().getName()
                    + ": onStart threw an exception", ex);
        }
        warnIfTooLong(SystemClock.elapsedRealtime() - time, service, "onStart");
    	}

也有些是直接运行mian方法，例如下，执行完后都会有如下ServiceManager.addService(xxx, m),把服务缓存起来。

	    mPackageManagerService = PackageManagerService.main(mSystemContext, installer,
                mFactoryTestMode != FactoryTest.FACTORY_TEST_OFF, mOnlyCore);

		public static PackageManagerService main(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
        // Self-check for initial settings.
        PackageManagerServiceCompilerMapping.checkProperties();

        PackageManagerService m = new PackageManagerService(context, installer,
                factoryTest, onlyCore);
        m.enableSystemUserPackages();
        ServiceManager.addService("package", m);
        final PackageManagerNative pmn = m.new PackageManagerNative();
        ServiceManager.addService("package_native", pmn);
        return m;
    }

而ServiceManager.addService方法添加的是一个name对应一个IBinder引用。因为这些服务都是可以提供给其他进程使用的。所以理所当然就是IBinder

 	/**
     * Place a new @a service called @a name into the service
     * manager.
     * 
     * @param name the name of the new service
     * @param service the service object
     */
    public static void addService(String name, IBinder service) {
        try {
            getIServiceManager().addService(name, service, false);
        } catch (RemoteException e) {
            Log.e(TAG, "error in addService", e);
        }
    }

在看PackageManagerService的构造方法，里面初始化了一个ServiceThread线程。其他的服务也都会有个线程

	public PackageManagerService(Context context, Installer installer,
	            boolean factoryTest, boolean onlyCore) {
	        ...
	        synchronized (mInstallLock) {
	        // writer
	        synchronized (mPackages) {
				//生成一个线程。作为PackageManagerService当前的主线程
	            mHandlerThread = new ServiceThread(TAG,
	                    Process.THREAD_PRIORITY_BACKGROUND, true /*allowIo*/);
	            mHandlerThread.start();
	            mHandler = new PackageHandler(mHandlerThread.getLooper());
	}

**在APK应用中能够直接交互的大部分系统服务都在systemServer进程中运行，比如WindowManagerServer,ActivityManagerSystemService,PackageManagerServer等常见服务，这些系统服务都是以一个线程的方式存在于SystemServer进程中。**