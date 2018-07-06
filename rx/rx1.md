

	Observable.create(new ObservableOnSubscribe<List<MusicInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<MusicInfo>> observableEmitter) throws Exception {
                MusicInfoDao dao=daoSession.getMusicInfoDao();
                DebugLog.d("subscribe:"+Thread.currentThread().getName());
                List<MusicInfo> list=dao.queryBuilder().orderAsc(com.music.bean.MusicInfoDao.Properties.TitleKey).list();
                if (list.isEmpty()){
                    DebugLog.d("本地数据没有，去手机多媒体数据库查询");
                    list.addAll(MusicModel.getInstance().sortMp3InfosByTitle(MusicApplication.getInstance()));
                }else{
                    DebugLog.d("从本地数据库获取 ");
                    MusicModel.getInstance().getMusicList().addAll(list);
                }
                musicInfoList.addAll(list);
                observableEmitter.onNext(list);
                observableEmitter.onComplete();
            }

        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(receiver);


	public class BaseObserver<T> implements Observer<T> {
	    @Override
	    public void onSubscribe(Disposable disposable) {
	        DebugLog.d("onSubscribe："+disposable.toString());
	        DebugLog.d(Thread.currentThread().getName());
	    }
	
	    @Override
	    public void onNext(T t) {
	        DebugLog.d("onNext");
	        DebugLog.d(Thread.currentThread().getName());
	    }
	
	    @Override
	    public void onError(Throwable throwable) {
	        DebugLog.d("onError");
	    }
	
	    @Override
	    public void onComplete() {
	        DebugLog.d("onComplete");
	        DebugLog.d(Thread.currentThread().getName());
	    }
	}
receiver 是一个BaseObserver子类

Observable.java

 	public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableCreate(source));
    }
##create方法
把我们创建的ObservableOnSubscribe对象包装到ObservableCreate对象,并且ObservableCreate也继承了Observable类

	public final class ObservableCreate<T> extends Observable<T> {
    	final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        this.source = source;
    }
RxJavaPlugins onAssembly方法 	
	
	public static <T> Observable<T> onAssembly(@NonNull Observable<T> source) {
        Function<? super Observable, ? extends Observable> f = onObservableAssembly;
        return f != null?(Observable)apply(f, source):source;
    }

onObservableAssembly是调用setOnObservableAssembly方法设置的

	public static void setOnObservableAssembly(@Nullable Function<? super Observable, ? extends Observable> onObservableAssembly) {
        if(lockdown) {
            throw new IllegalStateException("Plugins can't be changed anymore");
        } else {
            onObservableAssembly = onObservableAssembly;
        }
    }
我们没有调用这个方法因此这个对象为Null<br>
 	
	public static <T> Observable<T> create(ObservableOnSubscribe<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return RxJavaPlugins.onAssembly(new ObservableCreate(source));
    }
RxJavaPlugins onAssembly方法 返回的就是ObservableCreate对象<br>
##subscribeOn方法
2 create之后就是subscribeOn，因为我们想new ObservableOnSubscribe<List<MusicInfo>>这个里面的subscribe方法运行在子线程中，因此调用了subscribeOn(Schedulers.io())
查看因此调用了subscribeOn方法,此方法传入一个Scheduler对象

	public final Observable<T> subscribeOn(Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return RxJavaPlugins.onAssembly(new ObservableSubscribeOn(this, scheduler));
    }
ObservableSubscribeOn构造器把当前对象和我们创建的scheduler对象也传入进去，这个应该就是根据我们设置的线程，在我们设置的线程里面执行我们的方法，ObservableSubscribeOn类继承了AbstractObservableWithUpstream类，而AbstractObservableWithUpstream类继承Observable类，也就是说ObservableSubscribeOn也是Observable的子类<br>
	
	public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }
##observeOn方法
3 由于我们想要最终回调到主线程中，因此调用了observeOn(AndroidSchedulers.mainThread())方法

	public final Observable<T> observeOn(Scheduler scheduler) {
        return this.observeOn(scheduler, false, bufferSize());
    }
该方法调用了重载方法

 	public final Observable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn(this, scheduler, delayError, bufferSize));
    }
这个和前面那个设置线程的基本都差不多，同样的ObservableObserveOn也是Observable的子类

	public final class ObservableObserveOn<T> extends 	AbstractObservableWithUpstream<T, T> {
    	final Scheduler scheduler;
    	final boolean delayError;
    	final int bufferSize;

    public ObservableObserveOn(ObservableSource<T> source, Scheduler scheduler, boolean delayError, int bufferSize) {
        super(source);
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.bufferSize = bufferSize;
    }
终于到最后的subscribe方法<br>
##onSubscribe方法
4 到最后执行了subscribe方法，也就是封装到最后一个对象执行此方法，RxJavaPlugins.onSubscribe(this, observer)返回的就是observer对象;最后执行了subscribeActual方法，也就是封装到最后的一个对象执行了subscribe

	public final void subscribe(Observer<? super T> observer) {
        ObjectHelper.requireNonNull(observer, "observer is null");

        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);
            ObjectHelper.requireNonNull(observer, "Plugin returned null Observer");
            this.subscribeActual(observer);
        } catch (NullPointerException var4) {
            throw var4;
        } catch (Throwable var5) {
            Exceptions.throwIfFatal(var5);
            RxJavaPlugins.onError(var5);
            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(var5);
            throw npe;
        }
    }
执行subscribeActual方法时发现该方法是个抽象方法

	protected abstract void subscribeActual(Observer<? super T> var1);
总结前面的  
第1步 create方法把我们创建的ObservableOnSubscribe对象封装到ObservableCreate对象  
第2步 subscribeOn方法把ObservableCreate对象封装到ObservableSubscribeOn对象，  
第3步 observeOn方法，把ObservableSubscribeOn对象封装到ObservableObserveOn对象  
第4步 onSubscribe方法，其实就是调用ObservableObserveOn对象的方法了，该方法传入了一个Observer，这个Observer就是最后的回调对象  
封装的顺序如下<br>
>**我们创建的ObservableOnSubscribe->ObservableCreate->ObservableSubscribeOn->ObservableObserveOn**

了解这个封装顺序很重要，因为下面的调用前面是一步一步往前回调的。  
根据对象的封装情况，我们再从调用的顺序再一步一步往下分析<br>
查看我们的代码，我也是把这个回调给调用者。先记着这个receiver就是我们自己的回调对象

	public void sortMp3InfosByTitleByRx(BaseObserver<List<MusicInfo>> receiver){
		Observable.create(new ObservableOnSubscribe<List<MusicInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<MusicInfo>> observableEmitter) throws Exception {
                MusicInfoDao dao=daoSession.getMusicInfoDao();
                DebugLog.d("subscribe:"+Thread.currentThread().getName());
                List<MusicInfo> list=dao.queryBuilder().orderAsc(com.music.bean.MusicInfoDao.Properties.TitleKey).list();
                if (list.isEmpty()){
                    DebugLog.d("本地数据没有，去手机多媒体数据库查询");
                    list.addAll(MusicModel.getInstance().sortMp3InfosByTitle(MusicApplication.getInstance()));
                }else{
                    DebugLog.d("从本地数据库获取 ");
                    MusicModel.getInstance().getMusicList().addAll(list);
                }
                musicInfoList.addAll(list);
                observableEmitter.onNext(list);
                observableEmitter.onComplete();
            }

        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(receiver);
	}
由前面分析得知，此时执行subscribe方法的是一个ObservableObserveOn对象，因此我们继续查看
ObservableObserveOn对象里面的subscribe方法，发现里面没有这个方法，查看父类AbstractObservableWithUpstream，里面也没有这个方法，继续父类Observable,里面有这个方法  

>**总结：Observable子类调用subscribe后会调用自己subscribeActual方法**

	@SchedulerSupport("none")
    public final void subscribe(Observer<? super T> observer) {
        ObjectHelper.requireNonNull(observer, "observer is null");

        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);
            ObjectHelper.requireNonNull(observer, "Plugin returned null Observer");
            this.subscribeActual(observer);
        } catch (NullPointerException var4) {
            throw var4;
        } catch (Throwable var5) {
            Exceptions.throwIfFatal(var5);
            RxJavaPlugins.onError(var5);
            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(var5);
            throw npe;
        }
    }

查看ObservableObserveOn的subscribeActual方法,这个observer是我们传入的revicer对象,由前面分析得知，这个source对象就是ObservableSubscribeOn对象，因此这里又回调了ObservableSubscribeOn对象的subscribe方法，并且还创建了一个Worker对象，和我们的revicer对象封装到一个ObservableObserveOn.ObserveOnObserver对象里。

	protected void subscribeActual(Observer<? super T> observer) {
        if(this.scheduler instanceof TrampolineScheduler) {
            this.source.subscribe(observer);
        } else {
            Worker w = this.scheduler.createWorker();
            this.source.subscribe(new ObservableObserveOn.ObserveOnObserver(observer, w, this.delayError, this.bufferSize));
        }

    }
这里的scheduler是rxandroid包下面的HandlerScheduler类  
_**HandlerScheduler 笔记**_  
而这里创建一个worker对象

	private static final class HandlerWorker extends Worker {
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if(run == null) {
                throw new NullPointerException("run == null");
            } else if(unit == null) {
                throw new NullPointerException("unit == null");
            } else if(this.disposed) {
                return Disposables.disposed();
            } else {
                run = RxJavaPlugins.onSchedule(run);
                HandlerScheduler.ScheduledRunnable scheduled = new HandlerScheduler.ScheduledRunnable(this.handler, run);
                Message message = Message.obtain(this.handler, scheduled);
                message.obj = this;
                this.handler.sendMessageDelayed(message, unit.toMillis(delay));
                if(this.disposed) {
                    this.handler.removeCallbacks(scheduled);
                    return Disposables.disposed();
                } else {
                    return scheduled;
                }
            }
        }

        public void dispose() {
            this.disposed = true;
            this.handler.removeCallbacksAndMessages(this);
        }

        public boolean isDisposed() {
            return this.disposed;
        }
    }
这个类主要看schedule方法，很显然是利用handler回调到主线程,这里暂且先不管这个类，后面调用的时候再回过来看。

继续看ObservableObserveOn的subscribeActual方法

	protected void subscribeActual(Observer<? super T> observer) {
        if(this.scheduler instanceof TrampolineScheduler) {
            this.source.subscribe(observer);
        } else {
            Worker w = this.scheduler.createWorker();
            this.source.subscribe(new ObservableObserveOn.ObserveOnObserver(observer, w, this.delayError, this.bufferSize));
        }

    }

既然这个source是ObservableSubscribeOn对象，而这个ObservableSubscribeOn对象也是Observable的子类，由我们前面的分析得知，如果是Observable的子类，调用subsribe方法，就是调用subsribeActual方法，因此我们直接看ObservableSubscribeOn对象的subsribeActual方法，	  

	public void subscribeActual(Observer<? super T> s) {
        ObservableSubscribeOn.SubscribeOnObserver<T> parent = new ObservableSubscribeOn.SubscribeOnObserver(s);
        s.onSubscribe(parent);
        parent.setDisposable(this.scheduler.scheduleDirect(new ObservableSubscribeOn.SubscribeTask(parent)));
    }

这个SubscribeOnObserver类基本上就是对传入的s对象做了代理  
查看s.onSubscribe方法，这个s就是前面ObservableObserveOn.ObserveOnObserver对象，这个对象把我们的receiver对象传入了进去  
查看`ObservableObserveOn.ObserveOnObserver`对象的`onSubscribe`方法方法，由于传入进去的parent对象并不是QueueDisposable的子类，因此不会进入if语句里面，查看`this.actual.onSubscribe(this); `这个actual就是传入的对象就是我们的回调对象。

	public void onSubscribe(Disposable s) {
            if(DisposableHelper.validate(this.s, s)) {
                this.s = s;
                if(s instanceof QueueDisposable) {
                    QueueDisposable<T> qd = (QueueDisposable)s;
                    int m = qd.requestFusion(7);
                    if(m == 1) {
                        this.sourceMode = m;
                        this.queue = qd;
                        this.done = true;
                        this.actual.onSubscribe(this);
                        this.schedule();
                        return;
                    }

                    if(m == 2) {
                        this.sourceMode = m;
                        this.queue = qd;
                        this.actual.onSubscribe(this);
                        return;
                    }
                }

                this.queue = new SpscLinkedArrayQueue(this.bufferSize);
                this.actual.onSubscribe(this);
            }

        }

		
到这里就调用了我们BaseObserver的onSubscribe方法，再看后面是啥时候调用其他方法的  

继续看subscribeActual的方法，执行完`s.onSubscribe(parent);`后执行
 `parent.setDisposable(this.scheduler.scheduleDirect(new ObservableSubscribeOn.SubscribeTask(parent)));`  
这里把ObservableSubscribeOn.SubscribeOnObserver对象封装到ObservableSubscribeOn.SubscribeTask对象里  
ObservableSubscribeOn.SubscribeTask实现了Runnable接口，那主要代码在run方法里面  
run方法执行了ObservableSubscribeOn里面source的subscribe方法。我们知道ObservableSubscribeOn里面封装的是ObservableCreate对象，因此这个方法里面调用的ObservableCreate对象的subscribe方法，这个我们先放着，看后面的代码是怎么切换线程的并且调用这个run方法的  
_**SubscribeTask 笔记**_  
	final class SubscribeTask implements Runnable {
        private final ObservableSubscribeOn.SubscribeOnObserver<T> parent;

        SubscribeTask(ObservableSubscribeOn.SubscribeOnObserver<T> this$0) {
            this.parent = parent;
        }

        public void run() {
            ObservableSubscribeOn.this.source.subscribe(this.parent);
        }
    }
继续查看scheduler对象  
这个scheduler是创建ObservableSubscribeOn对象时创建的，找到前面创建ObservableSubscribeOn对象的代码

	public final Observable<T> observeOn(Scheduler scheduler) {
        return this.observeOn(scheduler, false, bufferSize());
    }
该方法调用了重载方法

 	public final Observable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        return RxJavaPlugins.onAssembly(new ObservableObserveOn(this, scheduler, delayError, bufferSize));
    }
创建new ObservableObserveOn(this, scheduler, delayError, bufferSize)，时这个scheuler就是我们调用Schedulers.io()传入进去的。


scheduler的scheduleDirect方法

	@NonNull
    public Disposable scheduleDirect(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
        Scheduler.Worker w = this.createWorker();
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        Scheduler.DisposeTask task = new Scheduler.DisposeTask(decoratedRun, w);
        w.schedule(task, delay, unit);
        return task;
    }
调用的子类生成的Worker对象的schedule方法，而我们用的是Schedulers.io()生成的是NewThreadScheduler类  
NewThreadScheduler类的创建的是一个NewThreadWorker。

	@NonNull
    public Worker createWorker() {
        return new NewThreadWorker(this.threadFactory);
    }

NewThreadWorker类的schedule方法，调用了重载方法，重载方法调用了scheduleActual方法

	@NonNull
    public Disposable schedule(@NonNull Runnable run) {
        return this.schedule(run, 0L, (TimeUnit)null);
    }

    @NonNull
    public Disposable schedule(@NonNull Runnable action, long delayTime, @NonNull TimeUnit unit) {
        return (Disposable)(this.disposed?EmptyDisposable.INSTANCE:this.scheduleActual(action, delayTime, unit, (DisposableContainer)null));
    }
 	@NonNull
    public ScheduledRunnable scheduleActual(Runnable run, long delayTime, @NonNull TimeUnit unit, @Nullable DisposableContainer parent) {
        Runnable decoratedRun = RxJavaPlugins.onSchedule(run);
        ScheduledRunnable sr = new ScheduledRunnable(decoratedRun, parent);
        if(parent != null && !parent.add(sr)) {
            return sr;
        } else {
            try {
                Object f;
                if(delayTime <= 0L) {
                    f = this.executor.submit(sr);
                } else {
                    f = this.executor.schedule(sr, delayTime, unit);
                }

                sr.setFuture((Future)f);
            } catch (RejectedExecutionException var10) {
                if(parent != null) {
                    parent.remove(sr);
                }

                RxJavaPlugins.onError(var10);
            }

            return sr;
        }
    }

前面在_**SubscribeTask 笔记**_记录，现在这里的参数run就是前面的SubscribeTask对象，这里又封装成一个ScheduledRunnable对象，不过这个ScheduledRunnable对象调用的也是传入run的方法。
再查看SubscribeTask类，调用的是ObservableSubscribeOn里面source 也就是ObservableCreate的方法  
  
	final class SubscribeTask implements Runnable {
        private final ObservableSubscribeOn.SubscribeOnObserver<T> parent;

        SubscribeTask(ObservableSubscribeOn.SubscribeOnObserver<T> this$0) {
            this.parent = parent;
        }

        public void run() {
            ObservableSubscribeOn.this.source.subscribe(this.parent);
        }
    }
根据前面总结得知，调用subscribe方法就是调用ObservableCreate的subscribeActual方法  
	protected void subscribeActual(Observer<? super T> observer) {
        ObservableCreate.CreateEmitter<T> parent = new ObservableCreate.CreateEmitter(observer);
        observer.onSubscribe(parent);

        try {
            this.source.subscribe(parent);
        } catch (Throwable var4) {
            Exceptions.throwIfFatal(var4);
            parent.onError(var4);
        }

    }
这里继续调用的source，也就是我们自己创建的一个ObservableOnSubscribe<List<MusicInfo>>对象的subsribe方法,因为这个调用是在异步线程里面调用的，因此我们的ObservableOnSubscribe<List<MusicInfo>>对象的subsribe方法也在异步线程运行。 

但是我们在subscribe方法里面也调用onNext和complete方法，为什么我们的BaseObserver的onNext方法和compete方法却回调到主线程了呢？  
此onNext和complete方法非我们的BaseObserver的onNext方法和compete方法   
ObservableCreate的subscribeActual方法里面把observer对象封装到parent对象里面，其实也就基本上是调用了observer的方法  
继续往前看调用者传入的这个参数，ObservableSubscribeOn的subscribeActual方法里面也是用了一个ObservableSubscribeOn.SubscribeOnObserver类对其进行了封装，也基本上是调用传入参数s的方法  

	public void subscribeActual(Observer<? super T> s) {
        ObservableSubscribeOn.SubscribeOnObserver<T> parent = new ObservableSubscribeOn.SubscribeOnObserver(s);
        s.onSubscribe(parent);
        parent.setDisposable(this.scheduler.scheduleDirect(new ObservableSubscribeOn.SubscribeTask(parent)));
    }
继续往前看ObservableObserveOn的subscribeActual方法，这里是一个ObservableObserveOn.ObserveOnObserver类对传入的oberver就行了封装，而这个observer才是我们的BaseObserver

	protected void subscribeActual(Observer<? super T> observer) {
        if(this.scheduler instanceof TrampolineScheduler) {
            this.source.subscribe(observer);
        } else {
            Worker w = this.scheduler.createWorker();
            this.source.subscribe(new ObservableObserveOn.ObserveOnObserver(observer, w, this.delayError, this.bufferSize));
        }

    }
查看ObservableObserveOn.ObserveOnObserver的onNext方法

	public void onNext(T t) {
            if(!this.done) {
                if(this.sourceMode != 2) {
                    this.queue.offer(t);
                }

                this.schedule();
            }
        }

调用了内部schedule方法  

	void schedule() {
            if(this.getAndIncrement() == 0) {
                this.worker.schedule(this);
            }

        }
这里也是调用了worker的schedule方法，并且把自己传入参数因为ObservableObserveOn.ObserveOnObserver实现了Runnable接口,最终也会调用run方法<br>
根据前面_**HandlerScheduler 笔记**_ ，这里执行的就是HandlerScheduler里面的schedule方法，HandlerScheduler继承了Scheduler类，并且创建的是一个HandleWorker对象，由名字可知，这里是利用Handler实现线程切换,查看内部代码也是利用Handler切换线程。
这个时候利用Handler已经把线程切换回来，所以下面的调用方法也就会在主线程中执行  
HandlerScheduler里面最终也会调用到ObservableObserveOn.ObserveOnObserver的run方法，  
由于没有这个outputFused这个值，因此走drainNormal方法

	public void run() {
            if(this.outputFused) {
                this.drainFused();
            } else {
                this.drainNormal();
            }

        }
里面调用了actual的next方法，而这个actual就是我们传入的BaseObserver，而此时，已经在主线程运行了。
这里方法首先调用checkTerminated方法，检查是否继续往下运行，由于done为false,这里继续走入另一个while循环
这个循环就一遍一遍的从queue里面获取参数数据，并且回调给BaseObserver的onNext方法，直到获取完毕，再调用checkTerminated方法时回调BaseObserver的onComplete方法，

	void drainNormal() {
            int missed = 1;
            SimpleQueue<T> q = this.queue;
            Observer a = this.actual;

            do {
                if(this.checkTerminated(this.done, q.isEmpty(), a)) {
                    return;
                }

                while(true) {
                    boolean d = this.done;

                    Object v;
                    try {
                        v = q.poll();
                    } catch (Throwable var7) {
                        Exceptions.throwIfFatal(var7);
                        this.s.dispose();
                        q.clear();
                        a.onError(var7);
                        this.worker.dispose();
                        return;
                    }

                    boolean empty = v == null;
                    if(this.checkTerminated(d, empty, a)) {
                        return;
                    }

                    if(empty) {
                        missed = this.addAndGet(-missed);
                        break;
                    }

                    a.onNext(v);
                }
            } while(missed != 0);

        }
每次调用这个方法都会往queue里面放入一个数据

	public void onNext(T t) {
            if(!this.done) {
                if(this.sourceMode != 2) {
                    this.queue.offer(t);
                }

                this.schedule();
            }
        }
在checkTerminated方法里，如果queue里面为null时，并且调用过onComplete()或者onError方法的话，就会回调给外部调用的onComplete方法和onError方法

	boolean checkTerminated(boolean d, boolean empty, Observer<? super T> a) {
            if(this.cancelled) {
                this.queue.clear();
                return true;
            } else {
                if(d) {
                    Throwable e = this.error;
                    if(this.delayError) {
                        if(empty) {
                            if(e != null) {
                                a.onError(e);
                            } else {
                                a.onComplete();
                            }

                            this.worker.dispose();
                            return true;
                        }
                    } else {
                        if(e != null) {
                            this.queue.clear();
                            a.onError(e);
                            this.worker.dispose();
                            return true;
                        }

                        if(empty) {
                            a.onComplete();
                            this.worker.dispose();
                            return true;
                        }
                    }
                }

                return false;
            }
        }

根据前面第3步得知，是把ObservableCreate封装了ObservableSubscribeOn，这个source就是ObservableCreate对象了，查看ObservableCreate对象的subscribe方法，发现里面没有这个方法，于是到父类AbstractObservableWithUpstream里面查找，也没找到，继续父类Observable查找。终于找到subscribe方法，这个和前面那个回调流程都一样，调用父类的subscribe后会调用自己subscribeActual方法，也就是说Observable的子类调用subscribe方法，其实真正的就是调用subscribeActual方法
Observable类的subscribe方法

	@SchedulerSupport("none")
    public final void subscribe(Observer<? super T> observer) {
        ObjectHelper.requireNonNull(observer, "observer is null");

        try {
            observer = RxJavaPlugins.onSubscribe(this, observer);
            ObjectHelper.requireNonNull(observer, "Plugin returned null Observer");
            this.subscribeActual(observer);
        } catch (NullPointerException var4) {
            throw var4;
        } catch (Throwable var5) {
            Exceptions.throwIfFatal(var5);
            RxJavaPlugins.onError(var5);
            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(var5);
            throw npe;
        }
    }
传入的是ObservableObserveOn.ObserveOnObserver对象<br>
前面我知道执行的是subscribeActual是个抽象方法，在ObservableSubscribeOn里面查看subscribeActual方法。
ObservableSubscribeOn的subscribeActual方法

	public void subscribeActual(Observer<? super T> s) {
        ObservableSubscribeOn.SubscribeOnObserver<T> parent = new ObservableSubscribeOn.SubscribeOnObserver(s);
        s.onSubscribe(parent);
        parent.setDisposable(this.scheduler.scheduleDirect(new ObservableSubscribeOn.SubscribeTask(parent)));
    }
SubscribeOnObserver 类是个代理类，执行的还是传入的对象

