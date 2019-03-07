#RX转换符 map

map()函数接受一个Func1类型的参数(就像这样map(Func1<? super T, ? extends R> func)),然后吧这个Func1应用到每一个由Observable发射的值上，将发射的只转换为我们期望的值  

用法，Observable.just(<T\>),map(Function<? super T, ? extends R\> mapper).subscriber(Observer<R\>) ；把T类型转换为R类型,最关键的就是map里面的apply方法,  
举个列子，现在我们传入的Integer类型数据，但是要对这些数据做处理，成String类型的数据，例子代码如下

	Observable.just(1,2,3,4).map(new Function<Integer,String>() {

            @Override
            public String apply(Integer arg0) throws Exception {
                // TODO Auto-generated method stub
                return ""+arg0;
            }
        }).subscribe(new Observer<String>() {


            @Override
            public void onSubscribe(Disposable disposable) {

            }

            @Override
            public void onNext(String t) {
          
                System.out.println("onNext:"+t);
            }

            @Override
            public void onError(Throwable e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub
                System.out.println("onComplete");
            }
        });

打印结果如下
>onNext:1  
>onNext:2  
>onNext:3  
>onNext:4  
>onComplete
  
确实奏效了  

现在查看源码

Observavle的just方法，这里调用了fromArray方法

	public static <T> Observable<T> just(T item1, T item2, T item3, T item4) {
        ObjectHelper.requireNonNull(item1, "The first item is null");
        ObjectHelper.requireNonNull(item2, "The second item is null");
        ObjectHelper.requireNonNull(item3, "The third item is null");
        ObjectHelper.requireNonNull(item4, "The fourth item is null");
        return fromArray(new Object[]{item1, item2, item3, item4});
    }
查看fromArray方法，里面封装成一个ObservableFromArray类

	public static <T> Observable<T> fromArray(T... items) {
        ObjectHelper.requireNonNull(items, "items is null");
        return items.length == 0?empty():(items.length == 1?just(items[0]):RxJavaPlugins.onAssembly(new ObservableFromArray(items)));
    }

继续map方法，把ObservableFromArray又封装到一个ObservableMap类

	public final <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return RxJavaPlugins.onAssembly(new ObservableMap(this, mapper));
    }

最后subscribe方法

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

也就是说封装的顺序ObservableFromArray->ObservableMap  
而ObservableMap 的subscribeActual方法，调用了Observavle的subscribe方法，我们都知道，Observavle会调用子类实现的subscribeActual方法，

ObservableMap 的subscribeActual方法

	public void subscribeActual(Observer<? super U> t) {
        this.source.subscribe(new ObservableMap.MapObserver(t, this.function));
    }

而这里的source是ObservableFromArray类，因此我们查看ObservableFromArray类的subscribeActual方法
ObservableFromArray类的subscribeActual方法。这里调用了传入对象的onSubscribe方法，我们传入的是ObservableMap.MapObserver对象

	public void subscribeActual(Observer<? super T> s) {
        ObservableFromArray.FromArrayDisposable<T> d = new ObservableFromArray.FromArrayDisposable(s, this.array);
        s.onSubscribe(d);
        if(!d.fusionMode) {
            d.run();
        }
    }
调用ObservableMap.MapObserver对象的onSubscribe方法，ObservableMap.MapObserver对象的onSubscribe又会调用我们传入的Observer<String>类的onSubscribe方法.接着调用ObservableFromArray.FromArrayDisposable类里面的run方法  
查看run方法，里面这个array就行just方法传入进来的数组，这里遍历了数组，因为构造器ObservableFromArray.FromArrayDisposable(s, this.array)的第一个参数传入的是ObservableMap.MapObserver对象，所以这里依次调用了ObservableMap.MapObserver的onNext方法。调用完onNext方法  

ObservableFromArray.FromArrayDisposable的run方法

	void run() {
            T[] a = this.array;
            int n = a.length;

            for(int i = 0; i < n && !this.isDisposed(); ++i) {
                T value = a[i];
                if(value == null) {
                    this.actual.onError(new NullPointerException("The " + i + "th element is null"));
                    return;
                }

                this.actual.onNext(value);
            }

            if(!this.isDisposed()) {
                this.actual.onComplete();
            }

        }

而ObservableMap.MapObserver构造器里面传入的是我们创建的new Function<Integer,String>对象，  
因此这里的mapper调用的就是我们Function对象里面的apply方法，这里就是把Integer类型的数据转换为String类型的数据，并且转换后的对象作为参数，传入subscribe(new Observer<String>())的onNext方法

ObservableMap.MapObserver的onNext方法

	public void onNext(T t) {
            if(!this.done) {
                if(this.sourceMode != 0) {
                    this.actual.onNext((Object)null);
                } else {
                    Object v;
                    try {
                        v = ObjectHelper.requireNonNull(this.mapper.apply(t), "The mapper function returned a null value.");
                    } catch (Throwable var4) {
                        this.fail(var4);
                        return;
                    }

                    this.actual.onNext(v);
                }
            }
        }

继续查看run方法，调用完onNext方法，后调用了onComplete方法

ObservableFromArray.FromArrayDisposable的run方法

	void run() {
            T[] a = this.array;
            int n = a.length;

            for(int i = 0; i < n && !this.isDisposed(); ++i) {
                T value = a[i];
                if(value == null) {
                    this.actual.onError(new NullPointerException("The " + i + "th element is null"));
                    return;
                }

                this.actual.onNext(value);
            }

            if(!this.isDisposed()) {
                this.actual.onComplete();
            }

        }