	

	new AsyncTask().execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 1 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("RunTask 1 finished");
					}
				}).start();
			}
		}, null).execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 2 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("RunTask 2 finished");
					}
				}).start();
			}
		}, null).execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 3 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("RunTask 3 finished");
					}
				}).start();
			}
		}, null);

打印

    add Runnable....mTasks:1
	execute Runnable....
	add Runnable....mTasks:1
	add Runnable....mTasks:2
	RunTask 1 execute
	execute Runnable....
	RunTask 2 execute
	execute Runnable....
	RunTask 3 execute
	RunTask 3 finished
	RunTask 2 finished
	RunTask 1 finished


去掉注释


	public class AsyncTask {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // We want at least 2 threads and at most 4 threads in the core pool,
    // preferring to have 1 less than the CPU count to avoid saturating
    // the CPU with background work
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    public static final Executor THREAD_POOL_EXECUTOR;

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    /**
     * An {@link Executor} that executes tasks one at a time in serial
     * order.  This serialization is global to a particular process.
     */
    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();


    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    private static final int TASK_TIME_OUT=10*0000;
    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {

            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {

                        if (mTasks.isEmpty()){
                           return;
                        }
                        if (r instanceof RunTask){
                            RunTask runTask= (RunTask) r;

                           for(;;){

                               if (runTask.isExeRunFinish())
                                   break;
                           }
                        }

                        scheduleNext();

                    }
                }
            });

            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                LogUtil.d("execute Runnable....");
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }
    public <Params> AsyncTask execute(RunTask<Params> runTask,Params params){
        runTask.onPreExecute(params);
        sDefaultExecutor.execute(runTask);
        return this;
    }
	}

继续执行

	new AsyncTask().execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 1 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							runFinish();
							System.out.println("RunTask 1 finished");
					}
				}).start();
			}
		}, null).execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 2 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(3000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							runFinish();
							System.out.println("RunTask 2 finished");
					}
				}).start();
			}
		}, null).execute(new RunTask() {
			@Override
			public void run() {
				System.out.println("RunTask 3 execute");
				new Thread(new Runnable() {
					@Override
					public void run() {
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							runFinish();
							System.out.println("RunTask 3 finished");
					}
				}).start();
			}
		}, null);


输出结果，串行了。

	execute Runnable....
	RunTask 1 execute
	runFinish ....
	RunTask 1 finished
	execute Runnable....
	RunTask 2 execute
	runFinish ....
	RunTask 2 finished
	execute Runnable....
	RunTask 3 execute
	runFinish ....
	RunTask 3 finished