#Glide 流程 4.+


 	Glide.with(fragment).setDefaultRequestOptions(options).load(path).into(target)

	//生成RequestManager对象
	public static RequestManager with(@NonNull Fragment fragment) {
	    return getRetriever(fragment.getActivity()).get(fragment);
	  }

	//设置一些参数
	public RequestManager setDefaultRequestOptions(@NonNull RequestOptions requestOptions) {
	    setRequestOptions(requestOptions);
	    return this;
	  }
	
	//又生成一个RequestBuilder对象
	 public RequestBuilder<Drawable> load(@Nullable String string) {
	    return asDrawable().load(string);
	  }
	
	
	public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
	    Util.assertMainThread();
	    Preconditions.checkNotNull(view);
	
	    RequestOptions requestOptions = this.requestOptions;
	    if (!requestOptions.isTransformationSet()
	        && requestOptions.isTransformationAllowed()
	        && view.getScaleType() != null) {
	      // Clone in this method so that if we use this RequestBuilder to load into a View and then
	      // into a different target, we don't retain the transformation applied based on the previous
	      // View's scale type.
	      switch (view.getScaleType()) {
	        case CENTER_CROP:
	          requestOptions = requestOptions.clone().optionalCenterCrop();
	          break;
	        case CENTER_INSIDE:
	          requestOptions = requestOptions.clone().optionalCenterInside();
	          break;
	        case FIT_CENTER:
	        case FIT_START:
	        case FIT_END:
	          requestOptions = requestOptions.clone().optionalFitCenter();
	          break;
	        case FIT_XY:
	          requestOptions = requestOptions.clone().optionalCenterInside();
	          break;
	        case CENTER:
	        case MATRIX:
	        default:
	          // Do nothing.
	      }
   		 }
		//调用into方法
		 return into(
		        glideContext.buildImageViewTarget(view, transcodeClass),
		        /*targetListener=*/ null,
		        requestOptions);
	  }

	private <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener,
      @NonNull RequestOptions options) {
	    Util.assertMainThread();
	    Preconditions.checkNotNull(target);
	    if (!isModelSet) {
	      throw new IllegalArgumentException("You must call #load() before calling #into()");
	    }
		
	    options = options.autoClone();
		//1 构建一个Request对象
	    Request request = buildRequest(target, targetListener, options);
	
	    Request previous = target.getRequest();
	    if (request.isEquivalentTo(previous)
	        && !isSkipMemoryCacheWithCompletePreviousRequest(options, previous)) {
	      request.recycle();
	      // If the request is completed, beginning again will ensure the result is re-delivered,
	      // triggering RequestListeners and Targets. If the request is failed, beginning again will
	      // restart the request, giving it another chance to complete. If the request is already
	      // running, we can let it continue running without interruption.
	      if (!Preconditions.checkNotNull(previous).isRunning()) {
	        // Use the previous request rather than the new one to allow for optimizations like skipping
	        // setting placeholders, tracking and un-tracking Targets, and obtaining View dimensions
	        // that are done in the individual Request.
	        previous.begin();
	      }
	      return target;
	    }
	
	    requestManager.clear(target);
	    target.setRequest(request);
		//2 调用requestManager.track方法
	    requestManager.track(target, request);
	
	    return target;
  	}



	 void track(Target<?> target, Request request) {
	    targetTracker.track(target);
		//调用RequestTracker的runRequest方法
	    requestTracker.runRequest(request);
	  }
	
	 public void runRequest(Request request) {
	    requests.add(request);
	    if (!isPaused) {
			//调用request的begin方法
	      request.begin();
	    } else {
	      pendingRequests.add(request);
	    }
	  }

	//看into方法，构造一个什么reqeust对象
	private <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener,
      @NonNull RequestOptions options) {
	    ....
	    options = options.autoClone();
		//1 构建一个Request对象
	    Request request = buildRequest(target, targetListener, options);
	
	   ....
  	}

	//此方法调用buildRequestRecursive方法	
	  private Request buildRequest(
	      Target<TranscodeType> target,
	      @Nullable RequestListener<TranscodeType> targetListener,
	      RequestOptions requestOptions) {
	    return buildRequestRecursive(
	        target,
	        targetListener,
	        /*parentCoordinator=*/ null,
	        transitionOptions,
	        requestOptions.getPriority(),
	        requestOptions.getOverrideWidth(),
	        requestOptions.getOverrideHeight(),
	        requestOptions);
	  }

	 private Request buildRequestRecursive(
	      Target<TranscodeType> target,
	      @Nullable RequestListener<TranscodeType> targetListener,
	      @Nullable RequestCoordinator parentCoordinator,
	      TransitionOptions<?, ? super TranscodeType> transitionOptions,
	      Priority priority,
	      int overrideWidth,
	      int overrideHeight,
	      RequestOptions requestOptions) {
	    // Build the ErrorRequestCoordinator first if necessary so we can update parentCoordinator.
	    ErrorRequestCoordinator errorRequestCoordinator = null;
	    if (errorBuilder != null) {
	      errorRequestCoordinator = new ErrorRequestCoordinator(parentCoordinator);
	      parentCoordinator = errorRequestCoordinator;
	    }
		//调用buildThumbnailRequestRecursive方法构造
	    Request mainRequest =
	        buildThumbnailRequestRecursive(
	            target,
	            targetListener,
	            parentCoordinator,
	            transitionOptions,
	            priority,
	            overrideWidth,
	            overrideHeight,
	            requestOptions);
	
	    if (errorRequestCoordinator == null) {
	      return mainRequest;
	    }
	
	    ...
	  }
	
	 private Request buildThumbnailRequestRecursive(
	      Target<TranscodeType> target,
	      RequestListener<TranscodeType> targetListener,
	      @Nullable RequestCoordinator parentCoordinator,
	      TransitionOptions<?, ? super TranscodeType> transitionOptions,
	      Priority priority,
	      int overrideWidth,
	      int overrideHeight,
	      RequestOptions requestOptions) {
	   ...
	
	      ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
	      Request fullRequest =
	          obtainRequest(
	              target,
	              targetListener,
	              requestOptions,
	              coordinator,
	              transitionOptions,
	              priority,
	              overrideWidth,
	              overrideHeight);
	     ....
	  }

	//基本都会调用这个方法,生成SingleRequest对象返回。
	private Request obtainRequest(
	      Target<TranscodeType> target,
	      RequestListener<TranscodeType> targetListener,
	      RequestOptions requestOptions,
	      RequestCoordinator requestCoordinator,
	      TransitionOptions<?, ? super TranscodeType> transitionOptions,
	      Priority priority,
	      int overrideWidth,
	      int overrideHeight) {
	    return SingleRequest.obtain(
	        context,
	        glideContext,
	        model,
	        transcodeClass,
	        requestOptions,
	        overrideWidth,
	        overrideHeight,
	        priority,
	        target,
	        targetListener,
	        requestListener,
	        requestCoordinator,
	        glideContext.getEngine(),
	        transitionOptions.getTransitionFactory());
	  }

	//前面看过Into里面最终会调用Request的begin方法。现在这个Reqeust又是SingleReqeust对象。就看它的begin方法
	@Override
	  public void begin() {
	    assertNotCallingCallbacks();
	    stateVerifier.throwIfRecycled();
	    startTime = LogTime.getLogTime();
	    if (model == null) {
	      if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
	        width = overrideWidth;
	        height = overrideHeight;
	      }
	      // Only log at more verbose log levels if the user has set a fallback drawable, because
	      // fallback Drawables indicate the user expects null models occasionally.
	      int logLevel = getFallbackDrawable() == null ? Log.WARN : Log.DEBUG;
	      onLoadFailed(new GlideException("Received null model"), logLevel);
	      return;
	    }
		//1 处理各种状态
	    if (status == Status.RUNNING) {
	      throw new IllegalArgumentException("Cannot restart a running request");
	    }
	
	    if (status == Status.COMPLETE) {
	      onResourceReady(resource, DataSource.MEMORY_CACHE);
	      return;
	    }
		//2 把状态设置为WAITING_FOR_SIZE
	    status = Status.WAITING_FOR_SIZE;
	    if (Util.isValidDimensions(overrideWidth, overrideHeight)) {
	      onSizeReady(overrideWidth, overrideHeight);
	    } else {
	      target.getSize(this);
	    }
	
	    if ((status == Status.RUNNING || status == Status.WAITING_FOR_SIZE)
	        && canNotifyStatusChanged()) {
	      target.onLoadStarted(getPlaceholderDrawable());
	    }
	    if (IS_VERBOSE_LOGGABLE) {
	      logV("finished run method in " + LogTime.getElapsedMillis(startTime));
	    }
	  }

	 @Override
	  public void onSizeReady(int width, int height) {
	    stateVerifier.throwIfRecycled();
	    if (IS_VERBOSE_LOGGABLE) {
	      logV("Got onSizeReady in " + LogTime.getElapsedMillis(startTime));
	    }
	    if (status != Status.WAITING_FOR_SIZE) {
	      return;
	    }
		//把状态设置为RUNNING
	    status = Status.RUNNING;
	
	    float sizeMultiplier = requestOptions.getSizeMultiplier();
	    this.width = maybeApplySizeMultiplier(width, sizeMultiplier);
	    this.height = maybeApplySizeMultiplier(height, sizeMultiplier);
	
	    if (IS_VERBOSE_LOGGABLE) {
	      logV("finished setup for calling load in " + LogTime.getElapsedMillis(startTime));
	    }
		//调用Engine的load方法
	    loadStatus = engine.load(
	        glideContext,
	        model,
	        requestOptions.getSignature(),
	        this.width,
	        this.height,
	        requestOptions.getResourceClass(),
	        transcodeClass,
	        priority,
	        requestOptions.getDiskCacheStrategy(),
	        requestOptions.getTransformations(),
	        requestOptions.isTransformationRequired(),
	        requestOptions.isScaleOnlyOrNoTransform(),
	        requestOptions.getOptions(),
	        requestOptions.isMemoryCacheable(),
	        requestOptions.getUseUnlimitedSourceGeneratorsPool(),
	        requestOptions.getUseAnimationPool(),
	        requestOptions.getOnlyRetrieveFromCache(),
	        this);
	
	    // This is a hack that's only useful for testing right now where loads complete synchronously
	    // even though under any executor running on any thread but the main thread, the load would
	    // have completed asynchronously.
	    if (status != Status.RUNNING) {
	      loadStatus = null;
	    }
	    if (IS_VERBOSE_LOGGABLE) {
	      logV("finished onSizeReady in " + LogTime.getElapsedMillis(startTime));
	    }
	  }

Engine的load方法，生成了一个DecodeJob对象放入EngineJob对象，并放入LoadStatus里面，返回。

	//省略参数
	public <R> LoadStatus load(...) {
	    Util.assertMainThread();
	    long startTime = LogTime.getLogTime();
	
	    EngineKey key = keyFactory.buildKey(model, signature, width, height, transformations,
	        resourceClass, transcodeClass, options);
	
	    EngineResource<?> active = loadFromActiveResources(key, isMemoryCacheable);
	    if (active != null) {
	      cb.onResourceReady(active, DataSource.MEMORY_CACHE);
	      if (Log.isLoggable(TAG, Log.VERBOSE)) {
	        logWithTimeAndKey("Loaded resource from active resources", startTime, key);
	      }
	      return null;
	    }
		//从缓存中加载
	    EngineResource<?> cached = loadFromCache(key, isMemoryCacheable);
	    if (cached != null) {
	      cb.onResourceReady(cached, DataSource.MEMORY_CACHE);
	      if (Log.isLoggable(TAG, Log.VERBOSE)) {
	        logWithTimeAndKey("Loaded resource from cache", startTime, key);
	      }
	      return null;
	    }
		//根据key获取EngineJob。获取到。直接返回。没有的话利用参数生成
	    EngineJob<?> current = jobs.get(key, onlyRetrieveFromCache);
	    if (current != null) {
	      current.addCallback(cb);
	      if (Log.isLoggable(TAG, Log.VERBOSE)) {
	        logWithTimeAndKey("Added to existing load", startTime, key);
	      }
	      return new LoadStatus(cb, current);
	    }
	
	    EngineJob<R> engineJob =
	        engineJobFactory.build(
	            key,
	            isMemoryCacheable,
	            useUnlimitedSourceExecutorPool,
	            useAnimationPool,
	            onlyRetrieveFromCache);
	
	    DecodeJob<R> decodeJob =
	        decodeJobFactory.build(
	            glideContext,
	            model,
	            key,
	            signature,
	            width,
	            height,
	            resourceClass,
	            transcodeClass,
	            priority,
	            diskCacheStrategy,
	            transformations,
	            isTransformationRequired,
	            isScaleOnlyOrNoTransform,
	            onlyRetrieveFromCache,
	            options,
	            engineJob);
	
	    jobs.put(key, engineJob);
	
	    engineJob.addCallback(cb);
		//执行DecodeJob里面的线程池
	    engineJob.start(decodeJob);
	
	    if (Log.isLoggable(TAG, Log.VERBOSE)) {
	      logWithTimeAndKey("Started new load", startTime, key);
	    }
	    return new LoadStatus(cb, engineJob);
	  }
	//执行线程池
	public void start(DecodeJob<R> decodeJob) {
	    this.decodeJob = decodeJob;
	    GlideExecutor executor = decodeJob.willDecodeFromCache()
	        ? diskCacheExecutor
	        : getActiveSourceExecutor();
	    executor.execute(decodeJob);
	  }


	DecodeJob实现了Runnable接口，因此直接看run方法
	public void run() {
	   
	    DataFetcher<?> localFetcher = currentFetcher;
	    try {
	      if (isCancelled) {
	        notifyFailed();
	        return;
	      }
	      runWrapped();
	    } catch (Throwable t) {
	     ...
	    } finally {
	      ...
	    }
  	}

	private void runWrapped() {
	     switch (runReason) {
	      case INITIALIZE:
	        stage = getNextStage(Stage.INITIALIZE);
	        currentGenerator = getNextGenerator();
	        runGenerators();
	        break;
	      case SWITCH_TO_SOURCE_SERVICE:
	        runGenerators();
	        break;
	      case DECODE_DATA:
	        decodeFromRetrievedData();
	        break;
	      default:
	        throw new IllegalStateException("Unrecognized run reason: " + runReason);
	    }
	  }