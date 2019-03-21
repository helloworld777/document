#Picasso流程解析



首先从调用代码看起

	// Trigger the download of the URL asynchronously into the image view.
	
	Picasso.with(context)
	.load(url)
	.placeholder(R.drawable.placeholder)
	.error(R.drawable.error)
	.resizeDimen(R.dimen.list_detail_image_size,R.dimen.list_detail_image_size)
	.centerInside()
	.into(holder.image);

很显然是单例模式

	  public static Picasso with(Context context) {
        if (singleton == null) {
            Class var1 = Picasso.class;
            synchronized(Picasso.class) {
                if (singleton == null) {
                    singleton = (new Picasso.Builder(context)).build();
                }
            }
        }

        return singleton;
   	 }


再看`load`方法,对参数做了检查，调用了一个重载方法，返回的是一个`RequestCreator`对象

	public RequestCreator load(@Nullable String path) {
	         if(path ==null) {
	              return new RequestCreator(this, null,0);
	            }
	         if(path.trim().length() ==0) {
	             throw new IllegalArgumentException("Path must not be empty.");
	         }
	
	          return load(Uri.parse(path));
	}

将`String`转换为`Uri`,直接生成一个`RequestCreator`对象，并把uri放入里面然后返回。后面的方法就在该对象里面调用了

	public  RequestCreator load(@Nullable Uri uri) {
	      return new RequestCreator(this,uri,0);
	}
	
而这个uri。则放入到data里面，这个data是，Request的一个内部类Builder.

	RequestCreator(Picasso picasso, Uri uri, int resourceId) {
        if (picasso.shutdown) {
            throw new IllegalStateException("Picasso instance already shut down. Cannot submit new requests.");
        } else {
            this.picasso = picasso;
            this.data = new Builder(uri, resourceId, picasso.defaultBitmapConfig);
        }
    }

再往下看。`placeholder`方法就是设置还未下载图片之前占位资源id，这样先显示的就是这张图片

	public RequestCreator placeholder(@DrawableRes int placeholderResId) {
			//默认这个值为true,如果调用noPlaceholder方法则为false
	       if(!setPlaceholder) {
	               throw new IllegalStateException("Already explicitly declared as no placeholder.");
	
	         }
	
	        if(placeholderResId ==0) {
	              throw new IllegalArgumentException("Placeholder image resource invalid.");
	          }
				//如果设置过placeholderDrawable,再调用这个方法就会抛异常。
	          if(placeholderDrawable!=null) {
	             throw new IllegalStateException("Placeholder image already set.");
	          }

	         this.placeholderResId= placeholderResId;
	
	         return this;
	
	    }

`error`方法设置如果发生错误是显示资源Id，和`placeholder`方法一样。

	/** An error drawable to be used if the request image could not be loaded. */
	
	public RequestCreator error(@DrawableRes int  errorResId) {
	
		if(errorResId ==0) {
			throw new  IllegalArgumentException("Error image resource invalid.");
		}
		if(errorDrawable!=null) {
			throw new  IllegalStateException("Error image already set.");
		}
		this.errorResId= errorResId;
		return this;
	
	}

接下来是`resizeDimen`方法,获取宽高。

	 public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
	        Resources resources = this.picasso.context.getResources();
	        int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
	        int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
	        return this.resize(targetWidth, targetHeight);
	    }

然后把宽高放到data里面，这个data是Request的内部类Builder.

	public RequestCreator resize(int targetWidth, int targetHeight) {
	        this.data.resize(targetWidth, targetHeight);
	        return this;
	    }
`centerInside`方法，也是调用data的方法。

	 public RequestCreator centerInside() {
	        this.data.centerInside();
	        return this;
	    }

最后看`into`方法。调用重载方法
		
	 public void into(ImageView target) {
	        this.into(target, (Callback)null);
	  }

这个方法代码比较长。
>首先检查是否在主线程调用。如果不是则直接抛异常。因为后面要设置控件占位图片，或者从缓存中读取图片设置给控件，所有该方法一定要在主线程调用。<br>
>然后一系列参数判断。不过默认的值都会走到生成reqeust  
>利用request的信息生成key,因为缓存就是根据该key去获取的。  
>默认内存缓存是开启的。从内存缓存中获取，获取到了直接设置返回，啥事也就没有了。  
>没有缓存则判断是否设置了占位图片，如果设置了则先显示占位图片，根据生成一个Action.调用picasso的方法添加

	public void into(ImageView target, Callback callback) {
	        long started = System.nanoTime();
			//检查是否在主线程。不是则抛出异常。
	        Utils.checkMain();
	        if (target == null) {
	            throw new IllegalArgumentException("Target must not be null.");
			//这里是查看data里面是否有uri.或者resourceId.因为前面构造RequestCreator的时候把uri传给了data.所以hasImage返回true
	        } else if (!this.data.hasImage()) {
	            this.picasso.cancelRequest(target);
	            if (this.setPlaceholder) {
	                PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
	            }
	
	        } else {
				//默认这个值是false.
	            if (this.deferred) {
	                if (this.data.hasSize()) {
	                    throw new IllegalStateException("Fit cannot be used with resize.");
	                }
	                int width = target.getWidth();
	                int height = target.getHeight();
	                if (width == 0 || height == 0) {
	                    if (this.setPlaceholder) {
	                        PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
	                    }
	                    this.picasso.defer(target, new DeferredRequestCreator(this, target, callback));
	                    return;
	                }
	
	                this.data.resize(width, height);
	            }
				//利用data创造一个Request.因为之前设置的属性都设置到data里面。所以这里创建的时候传到了Request里面
	            Request request = this.createRequest(started);
				//利用request的一些信息生成key.
	            String requestKey = Utils.createKey(request);
				//如果设置了NO_CACHE，则不从内存缓存中获取。否则先从内存缓存中获取。默认缓存是LruCache
	            if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy)) {
	                Bitmap bitmap = this.picasso.quickMemoryCacheCheck(requestKey);
					//如果缓存中有。则取消请求。设置到目标控件上
	                if (bitmap != null) {
	                    this.picasso.cancelRequest(target);
	                    PicassoDrawable.setBitmap(target, this.picasso.context, bitmap, LoadedFrom.MEMORY, this.noFade, this.picasso.indicatorsEnabled);
	                    if (this.picasso.loggingEnabled) {
	                        Utils.log("Main", "completed", request.plainId(), "from " + LoadedFrom.MEMORY);
	                    }
	
	                    if (callback != null) {
	                        callback.onSuccess();
	                    }
	
	                    return;
	                }
	            }
				//如果设置了占位图片，先设置占位图片
	            if (this.setPlaceholder) {
	                PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
	            }
				//生成一个Action
	            Action action = new ImageViewAction(this.picasso, target, request, this.memoryPolicy, this.networkPolicy, this.errorResId, this.errorDrawable, requestKey, this.tag, callback, this.noFade);
				//添加到队列并且提交
	            this.picasso.enqueueAndSubmit(action);
	        }
	    }

小结:`with`方法是创建一个`Picasso`单例出来，`load`方法是创建一个`RequestCreator`对象出来。后面的所有链式调用就都是在`RequestCreator`对象里面。`RequestCreator`对象主要是接收参数，并且校验参数。并且从内存缓存里面，如果存在则不用去网络请求，也就没`Request`和`Action`的什么事了。如果不存在，把一些必要的参数放到`Action`对象，然后提交出去。

下面再看看怎么处理网络请求的，接着上面从`Picasso`的`enqueueAndSubmit`看起


	void enqueueAndSubmit(Action action) {
	        Object target = action.getTarget();
			//如果已经有了该action.则取消。再重新放入
	        if (target != null && this.targetToAction.get(target) != action) {
	            this.cancelExistingRequest(target);
	            this.targetToAction.put(target, action);
	        }
	
	        this.submit(action);
	    }

	void submit(Action action) {
	        this.dispatcher.dispatchSubmit(action);
	 }

调用`Dispatcher`对象的`dispatchSubmit`方法,发送一个`what`为1的`message`,该`handler`是一个内部类，并且不是获取主线程的`loop`。而是获取内部类`DispatcherThread`的`loop`。该类继承`HandlerThread`。也就是说`handler`是在子线程中处理`Message`的。

	 void dispatchSubmit(Action action) {
	        this.handler.sendMessage(this.handler.obtainMessage(1, action));
	    }


看`handlerMessage`方法里面处理消息为1的地方,调用了`performSubmit`方法。此时已经切换到子线程中运行了

	public void handleMessage(final Message msg) {
            Object tag;
            BitmapHunter hunter;
            Action action;
            switch(msg.what) {
            case 1:
                action = (Action)msg.obj;
                this.dispatcher.performSubmit(action);
                break;
            case 2:
调用重载方法

	 void performSubmit(Action action) {
	        this.performSubmit(action, true);
	    }
	

注释1.判断当前action的是否暂停了。如果暂停了。就不去请求。这个在配合列表使用更好。因为当滑动列表时，会产生很多请求，这个时候如果滑动的时候调用pauseTag方法，则就不会请求服务器。滑动完成后调用resumeTag，又会去请求服务器

注释2 判断是否有相同的action。这样就不用重复添加。  
注释3则利用action，和其他信息生成一个BitmapHunter对象。该对象继承了Runnable对象。并且把该对象放入线程池中。既然是Runnable对象。那就看run方法

	  void performSubmit(Action action, boolean dismissFailed) {
			//1判断集合Set中是否存在，由于没设置过，因此走else.如果设置了。则就不会请求。如果在列表中滑动会生成很多action。这样不可见的ImageView就可以设置pausedTag。
	        if (this.pausedTags.contains(action.getTag())) {
	            this.pausedActions.put(action.getTarget(), action);
	            if (action.getPicasso().loggingEnabled) {
	                Utils.log("Dispatcher", "paused", action.request.logId(), "because tag '" + action.getTag() + "' is paused");
	            }
	
	        } else {
				//2 由于没设置过，故为null
	            BitmapHunter hunter = (BitmapHunter)this.hunterMap.get(action.getKey());
	            if (hunter != null) {
	                hunter.attach(action);
				//线程池是否关闭了
	            } else if (this.service.isShutdown()) {
	                if (action.getPicasso().loggingEnabled) {
	                    Utils.log("Dispatcher", "ignored", action.request.logId(), "because shut down");
	                }
	
	            } else {
					//3生成了一个BitmapHunter对象，该对象继承了Runnable
	                hunter = BitmapHunter.forRequest(action.getPicasso(), this, this.cache, this.stats, action);
					//提交到线程池
	                hunter.future = this.service.submit(hunter);
					
					//把action的key 和hunter绑定在集合
	                this.hunterMap.put(action.getKey(), hunter);
	                if (dismissFailed) {
	                    this.failedActions.remove(action.getTarget());
	                }
	
	                if (action.getPicasso().loggingEnabled) {
	                    Utils.log("Dispatcher", "enqueued", action.request.logId());
	                }
	
	            }
	        }
	    }

`BitmapHunter`对象的`run`方法，设置线程名。然后调用`hunt`方法去获取。然后返回相应的回调。如果出现了异常，根据不同异常执行重试或者回调失败

	 public void run() {
	        try {
				//设置线程名
	            updateThreadName(this.data);
	            if (this.picasso.loggingEnabled) {
	                Utils.log("Hunter", "executing", Utils.getLogIdsForHunter(this));
	            }
	
	            this.result = this.hunt();
	            if (this.result == null) {
	                this.dispatcher.dispatchFailed(this);
	            } else {
	                this.dispatcher.dispatchComplete(this);
	            }
	        } catch (ResponseException var10) {
	            if (!var10.localCacheOnly || var10.responseCode != 504) {
	                this.exception = var10;
	            }
	
	            this.dispatcher.dispatchFailed(this);
	        } catch (ContentLengthException var11) {
	            this.exception = var11;
				//重新提交请求。
	            this.dispatcher.dispatchRetry(this);
	        } catch (IOException var12) {
	            this.exception = var12;
				//重新提交请求。
	            this.dispatcher.dispatchRetry(this);
	        } catch (OutOfMemoryError var13) {
	            StringWriter writer = new StringWriter();
	            this.stats.createSnapshot().dump(new PrintWriter(writer));
	            this.exception = new RuntimeException(writer.toString(), var13);
	            this.dispatcher.dispatchFailed(this);
	        } catch (Exception var14) {
	            this.exception = var14;
	            this.dispatcher.dispatchFailed(this);
	        } finally {
	            Thread.currentThread().setName("Picasso-Idle");
	        }
	
	    }
	
`hunt`方法。这个方法比较长。看主要部分的，这里也先是取缓存中获取。如果存在，则直接返回。如果不存在，则调用requestHandler去处理。
	 Bitmap hunt() throws IOException {
	        Bitmap bitmap = null;
			//1 如果可以从缓存中获取。去缓存获取。
	        if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy)) {
	            bitmap = this.cache.get(this.key);
	            if (bitmap != null) {
	                this.stats.dispatchCacheHit();
	                this.loadedFrom = LoadedFrom.MEMORY;
	                if (this.picasso.loggingEnabled) {
	                    Utils.log("Hunter", "decoded", this.data.logId(), "from cache");
	                }
	
	                return bitmap;
	            }
	        }
	
	        this.data.networkPolicy = this.retryCount == 0 ? NetworkPolicy.OFFLINE.index : this.networkPolicy;
			//2 通过requestHandler去获取
	        Result result = this.requestHandler.load(this.data, this.networkPolicy);
	       ...
	        }
	
	        return bitmap;
	    }

requestHandler是创建BitmapHunter的时候传进来的。这里获取Picasso里面RequestHandler的集合。而requestHandlers里面有什么呢？

	  static BitmapHunter forRequest(Picasso picasso, Dispatcher dispatcher, Cache cache, Stats stats, Action action) {
	        Request request = action.getRequest();
	        List<RequestHandler> requestHandlers = picasso.getRequestHandlers();
	        int i = 0;
	
	        for(int count = requestHandlers.size(); i < count; ++i) {
	            RequestHandler requestHandler = (RequestHandler)requestHandlers.get(i);
	            if (requestHandler.canHandleRequest(request)) {
	                return new BitmapHunter(picasso, dispatcher, cache, stats, action, requestHandler);
	            }
	        }
	
	        return new BitmapHunter(picasso, dispatcher, cache, stats, action, ERRORING_HANDLER);
	    }
创建`Picasso`的时候就已经放置很多`RequestHandler`在里面,根据load方法传进来的字符串资源分别对应不一样的处理对象。这样我们就可以在load方法里面传入资源文件或者文件路径或者网络url等等
	
			List<RequestHandler> allRequestHandlers = new ArrayList(builtInHandlers + extraCount);
	        allRequestHandlers.add(new ResourceRequestHandler(context));
	        if (extraRequestHandlers != null) {
	            allRequestHandlers.addAll(extraRequestHandlers);
	        }
	
	        allRequestHandlers.add(new ContactsPhotoRequestHandler(context));
	        allRequestHandlers.add(new MediaStoreRequestHandler(context));
	        allRequestHandlers.add(new ContentStreamRequestHandler(context));
	        allRequestHandlers.add(new AssetRequestHandler(context));
	        allRequestHandlers.add(new FileRequestHandler(context));
	        allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader, stats));
	        this.requestHandlers = Collections.unmodifiableList(allRequestHandlers);

由于我们传进来的是url。所以这里的`requestHandler`就是`NetworkRequestHandler`处理

看`NetworkRequestHandler`方法处理,这个downloader也是Picasso类里面创建的。并且里面用的是OkHttp

	 public Result load(Request request, int networkPolicy) throws IOException {
	        Response response = this.downloader.load(request.uri, request.networkPolicy);
	        if (response == null) {
	            return null;
	        } else {
	            LoadedFrom loadedFrom = response.cached ? LoadedFrom.DISK : LoadedFrom.NETWORK;
	            Bitmap bitmap = response.getBitmap();
				//返回bitmap
	            if (bitmap != null) {
	                return new Result(bitmap, loadedFrom);
	            } else {
	                InputStream is = response.getInputStream();
	                if (is == null) {
	                    return null;
	                } else if (loadedFrom == LoadedFrom.DISK && response.getContentLength() == 0L) {
	                    Utils.closeQuietly(is);
	                    throw new NetworkRequestHandler.ContentLengthException("Received response with 0 content-length header.");
	                } else {
	                    if (loadedFrom == LoadedFrom.NETWORK && response.getContentLength() > 0L) {
	                        this.stats.dispatchDownloadFinished(response.getContentLength());
	                    }
	
	                    return new Result(is, loadedFrom);
	                }
	            }
	        }
	    }

继续看`hunt`方法

	 Bitmap hunt() throws IOException {
	        Bitmap bitmap = null;
	       ...
	        Result result = this.requestHandler.load(this.data, this.networkPolicy);
	        if (result != null) {
	            this.loadedFrom = result.getLoadedFrom();
	            this.exifRotation = result.getExifOrientation();
	            bitmap = result.getBitmap();
	            if (bitmap == null) {
	                InputStream is = result.getStream();
	
	                try {
	                    bitmap = decodeStream(is, this.data);
	                } finally {
	                    Utils.closeQuietly(is);
	                }
	            }
	        }
	
	        if (bitmap != null) {
	            if (this.picasso.loggingEnabled) {
	                Utils.log("Hunter", "decoded", this.data.logId());
	            }
	
	            this.stats.dispatchBitmapDecoded(bitmap);
				//如果需要缩放或者旋转处理。
	            if (this.data.needsTransformation() || this.exifRotation != 0) {
	                Object var10 = DECODE_LOCK;
	                synchronized(DECODE_LOCK) {
	                    if (this.data.needsMatrixTransform() || this.exifRotation != 0) {
	                        bitmap = transformResult(this.data, bitmap, this.exifRotation);
	                        if (this.picasso.loggingEnabled) {
	                            Utils.log("Hunter", "transformed", this.data.logId());
	                        }
	                    }
	
	                    if (this.data.hasCustomTransformations()) {
	                        bitmap = applyCustomTransformations(this.data.transformations, bitmap);
	                        if (this.picasso.loggingEnabled) {
	                            Utils.log("Hunter", "transformed", this.data.logId(), "from custom transformations");
	                        }
	                    }
	                }
	
	                if (bitmap != null) {
	                    this.stats.dispatchBitmapTransformed(bitmap);
	                }
	            }
	        }
	
	        return bitmap;
	    }

继续往前看`hunt`调用的地方，如果已经获取到bitmap了，调用`dispatcher`的`dispatchComplete`方法

		 public void run() {
	        try {
				//设置线程名
	            updateThreadName(this.data);
	            if (this.picasso.loggingEnabled) {
	                Utils.log("Hunter", "executing", Utils.getLogIdsForHunter(this));
	            }
	
	            this.result = this.hunt();
	            if (this.result == null) {
	                this.dispatcher.dispatchFailed(this);
	            } else {
	                this.dispatcher.dispatchComplete(this);
	            }
	        } 
			...
	
	    }
	
`Dispatcher`的`dispatchComplete`方法

	void dispatchComplete(BitmapHunter hunter) {
	        this.handler.sendMessage(this.handler.obtainMessage(4, hunter));
	    }

	  case 4:````````
	   hunter = (BitmapHunter)msg.obj;
	   this.dispatcher.performComplete(hunter);
	    break;

调用`performComplete`方法，设置了缓存

	 void performComplete(BitmapHunter hunter) {
	        if (MemoryPolicy.shouldWriteToMemoryCache(hunter.getMemoryPolicy())) {
	            this.cache.set(hunter.getKey(), hunter.getResult());
	        }
	
	        this.hunterMap.remove(hunter.getKey());
	        this.batch(hunter);
	        if (hunter.getPicasso().loggingEnabled) {
	            Utils.log("Dispatcher", "batched", Utils.getLogIdsForHunter(hunter), "for completion");
	        }
	
	    }
又发了一个消息

	  private void batch(BitmapHunter hunter) {
	        if (!hunter.isCancelled()) {
	            this.batch.add(hunter);
	            if (!this.handler.hasMessages(7)) {
	                this.handler.sendEmptyMessageDelayed(7, 200L);
	            }
	
	        }
	    }

处理消息。调用了`performBatchComplete`

	 public void handleMessage(final Message msg) {
            Object tag;
            BitmapHunter hunter;
            Action action;
            switch(msg.what) {
           ...
            case 7:
                this.dispatcher.performBatchComplete();
                break;

发到主线程中去了，这主线程是`Pacasso`里面的

	  void performBatchComplete() {
	        List<BitmapHunter> copy = new ArrayList(this.batch);
	        this.batch.clear();
	        this.mainThreadHandler.sendMessage(this.mainThreadHandler.obtainMessage(8, copy));
	        this.logBatch(copy);
	    }

Picasso里面的`handleMessage`方法

	  public void handleMessage(Message msg) {
	           ...
	            case 8:
	                batch = (List)msg.obj;
	                i = 0;
	
	                for(n = batch.size(); i < n; ++i) {
	                    BitmapHunter hunter = (BitmapHunter)batch.get(i);
	                    hunter.picasso.complete(hunter);
	                }
	
	                return;
调用了`complete`方法，我们这里的`single`是个`ImageViewAction`.后`deliverAction`会调用`ImageViewAction`里面的`complete`方法

	 void complete(BitmapHunter hunter) {
	        Action single = hunter.getAction();
	        List<Action> joined = hunter.getActions();
	        boolean hasMultiple = joined != null && !joined.isEmpty();
	        boolean shouldDeliver = single != null || hasMultiple;
	        if (shouldDeliver) {
	            Uri uri = hunter.getData().uri;
	            Exception exception = hunter.getException();
	            Bitmap result = hunter.getResult();
	            Picasso.LoadedFrom from = hunter.getLoadedFrom();
	            if (single != null) {
	                this.deliverAction(result, from, single);
	            }
	
	            if (hasMultiple) {
	                int i = 0;
	
	                for(int n = joined.size(); i < n; ++i) {
	                    Action join = (Action)joined.get(i);
	                    this.deliverAction(result, from, join);
	                }
	            }
	
	            if (this.listener != null && exception != null) {
	                this.listener.onImageLoadFailed(this, uri, exception);
	            }
	
	        }
	    }

`ImageViewAction`的`complete`方法。


	   public void complete(Bitmap result, LoadedFrom from) {
	        if (result == null) {
	            throw new AssertionError(String.format("Attempted to complete action with no result!\n%s", this));
	        } else {
	            ImageView target = (ImageView)this.target.get();
	            if (target != null) {
	                Context context = this.picasso.context;
	                boolean indicatorsEnabled = this.picasso.indicatorsEnabled;
	                PicassoDrawable.setBitmap(target, context, result, from, this.noFade, indicatorsEnabled);
	                if (this.callback != null) {
	                    this.callback.onSuccess();
	                }
	
	            }
	        }
	    }


	}


至此完成  
小结:`Dispatcher`里面有个`DispatcherThread`线程，该线程继承`HandlerThread`类，里面的`DispatcherHandler`类用的就是`DispatcherThread`的`loop`.这样就可以方便的把代码切换到子线程。主要的工作是在`BitmapHunter`类的`run`方法。并且里面的下载用的是`Okhttp`。下载完成后如果设置了大小的，或者自定义处理的。都在获取后操作`bitmap`的。