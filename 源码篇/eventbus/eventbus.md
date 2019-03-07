 	/**
     * Registers the given subscriber to receive events. Subscribers must call {@link #unregister(Object)} once they
     * are no longer interested in receiving events.
     * <p/>
     * Subscribers have event handling methods that must be annotated by {@link Subscribe}.
     * The {@link Subscribe} annotation also allows configuration like {@link
     * ThreadMode} and priority.
     */
    public void register(Object subscriber) {
		//获取对象类型
        Class<?> subscriberClass = subscriber.getClass();
		//找到类里面标记Subscribe枚举的方法，并封装成SubscriberMethod对象
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

查找类里面用Subscribe枚举的方法在  
`SubscriberMethodFinder`类的`findSubscriberMethods`方法,首先从缓存中查找存在，如果没有，则去反射获取。缓存的key，就是参数的类类型。SubscriberMethod类就是封装了该类里面有Subscribe枚举标识方法。

	 List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
			//从缓存中查找
	        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
	        if (subscriberMethods != null) {
	            return subscriberMethods;
	        }
			//默认值为false
	        if (ignoreGeneratedIndex) {
	            subscriberMethods = findUsingReflection(subscriberClass);
	        } else {
	            subscriberMethods = findUsingInfo(subscriberClass);
	        }
			//如果当前类和父类中都没有用Subscribe标记的方法。抛出异常
	        if (subscriberMethods.isEmpty()) {
	            throw new EventBusException("Subscriber " + subscriberClass
	                    + " and its super classes have no public methods with the @Subscribe annotation");
	        } else {
				//放入缓存
	            METHOD_CACHE.put(subscriberClass, subscriberMethods);
	            return subscriberMethods;
	        }
	    }
`findSubscriberMethods`方法里面调用的是`findUsingInfo`方法

	private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
	        FindState findState = prepareFindState();
			//初始化里面的值
	        findState.initForSubscriber(subscriberClass);
			//findState.clazz就是subscriberClass
			？、一直往上查找父类，直到父类是系统类为止
	        while (findState.clazz != null) {
				//获取subscriberInfo对象
	            findState.subscriberInfo = getSubscriberInfo(findState);
	            if (findState.subscriberInfo != null) {
	                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
	                for (SubscriberMethod subscriberMethod : array) {
	                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
	                        findState.subscriberMethods.add(subscriberMethod);
	                    }
	                }
	            } else {
	                findUsingReflectionInSingleClass(findState);
	            }
				一直往上查找父类，直到父类是系统类为止
	            findState.moveToSuperclass();
	        }
	        return getMethodsAndRelease(findState);
	    }

`prepareFindState`方法

这个方法主要是生成FindState对象，首先看复用池中有没有可以复用的，如果没有，直接生成一个。这个和Message.obtain生成Message的方式一样。
		
	  private FindState prepareFindState() {
	        synchronized (FIND_STATE_POOL) {
	            for (int i = 0; i < POOL_SIZE; i++) {
					//从复用池中拿出一个。如果有返回。如果没有new一个
	                FindState state = FIND_STATE_POOL[i];
	                if (state != null) {
	                    FIND_STATE_POOL[i] = null;
	                    return state;
	                }
	            }
	        }
	        return new FindState();
	    }
`initForSubscriber`

初始化FindState对象里面的属性，this.subscriberClass和clazz类型就是前面register方法传进来的对象类型

	 void initForSubscriber(Class<?> subscriberClass) {
	            this.subscriberClass = clazz = subscriberClass;
	            skipSuperClasses = false;
	            subscriberInfo = null;
	        }

`getSubscriberInfo`

由于前面初始化了SubscriberInfo对象里面属性值，subscriberInfo就是null。因此这里看后面的从subscriberInfoIndexes 缓存里面获取，如果找到，返回集合里面的对象。注意这里的subscriberInfoIndexes是个List集合。

提问1 为什么这里不用Map集合？

	 private SubscriberInfo getSubscriberInfo(FindState findState) {
	        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
	            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
	            if (findState.clazz == superclassInfo.getSubscriberClass()) {
	                return superclassInfo;
	            }
	        }
	        if (subscriberInfoIndexes != null) {
	            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
	                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
	                if (info != null) {
	                    return info;
	                }
	            }
	        }
	        return null;
	    }

再看findUsingInfo方法

	private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
		        FindState findState = prepareFindState();
				//初始化里面的值
		        findState.initForSubscriber(subscriberClass);
				//findState.clazz就是subscriberClass
		        while (findState.clazz != null) {
					//获取subscriberInfo对象
		            findState.subscriberInfo = getSubscriberInfo(findState);
					//subscriberInfoIndexes缓存中没有subscriberInfo对象时
		            if (findState.subscriberInfo != null) {
		                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
		                for (SubscriberMethod subscriberMethod : array) {
		                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
		                        findState.subscriberMethods.add(subscriberMethod);
		                    }
		                }
		            } else {
					
		                findUsingReflectionInSingleClass(findState);
		            }
		            findState.moveToSuperclass();
		        }
		        return getMethodsAndRelease(findState);
		    }

`findUsingReflectionInSingleClass`方法。
这个方法才是真正获取Subscribe标记的方法。

	 private void findUsingReflectionInSingleClass(FindState findState) {
	        Method[] methods;
	        try {
	            // This is faster than getMethods, especially when subscribers are fat classes like Activities
				//获取类里面所有的方法
	            methods = findState.clazz.getDeclaredMethods();
	        } catch (Throwable th) {
	            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
	            methods = findState.clazz.getMethods();
	            findState.skipSuperClasses = true;
	        }
			//遍历类里面所有的方法
	        for (Method method : methods) {
	            int modifiers = method.getModifiers();
				//权限修饰符，必须要public。并且非static 非abstract。否则抛出异常
	            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
					//获取方法的参数类型
	                Class<?>[] parameterTypes = method.getParameterTypes();
					//如果方法的参数只有一个
	                if (parameterTypes.length == 1) {
						//获取Subscribe枚举
	                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
	                    if (subscribeAnnotation != null) {
							//获取参数类型
	                        Class<?> eventType = parameterTypes[0];
							
							//由下面的checkAdd方法代码分析，checkAdd方法返回的是true
	                        if (findState.checkAdd(method, eventType)) {
	                            ThreadMode threadMode = subscribeAnnotation.threadMode();
								//找到所有了一个方法。并添加到findState对象的subscriberMethods集合中。等下这个集合就会放入缓存
	                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
	                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
	                        }
	                    }
					//如果方法前面有Subsrcibe标识。并且参数多于1个。如果设置strictMethodVerification为true,则直接抛出异常。默认strictMethodVerification为false。
	                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
	                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
	                    throw new EventBusException("@Subscribe method " + methodName +
	                            "must have exactly 1 parameter but has " + parameterTypes.length);
	                }
				//如果方法前面有Subsrcibe标识。并且不是public 修饰。或者是static 修饰 或者是abstract修饰。如果设置strictMethodVerification为true,则直接抛出异常。默认strictMethodVerification为false。

	            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
	                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
	                throw new EventBusException(methodName +
	                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
	            }
	        }
	    }


	从`anyMethodByEventType`集合中把参数类型当成key,方法对象当成value存入。put方法返回的值。如果有两个方法的接收的参数类型是一样的。第一个方法执行到这里时，由于集合中还没有method，返回Null,此方法返回true.第二个参数类型一样的方法调用此方法时，existing就是put返回的值是第一个方法对象。这个时候执行else了。else里面会执行两次checkAddWithMethodSignature方法。


	boolean checkAdd(Method method, Class<?> eventType) {
	            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
	            // Usually a subscriber doesn't have methods listening to the same event type.
	            Object existing = anyMethodByEventType.put(eventType, method);
	            if (existing == null) {
	                return true;
	            } else {
	                if (existing instanceof Method) {
	                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
	                        // Paranoia check
	                        throw new IllegalStateException();
	                    }
	                    // Put any non-Method object to "consume" the existing Method
	                    anyMethodByEventType.put(eventType, this);
	                }
	                return checkAddWithMethodSignature(method, eventType);
	            }
	        }
	根据方法的名字和参数的类型生成key.放入subscriberClassByMethodKey 集合中，value为该方法的类类型。第一次执行该方法subscriberClassByMethodKey里面没有该key对应的value。返回的是null.该方法返回的是ture.第二次调用该方法时，subscriberClassByMethodKey里面有key了。获取到的就是刚才放入的类类型。isAssignableFrom返回的是true。该方法返回的也是true.

	   private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
	            methodKeyBuilder.setLength(0);
	            methodKeyBuilder.append(method.getName());
	            methodKeyBuilder.append('>').append(eventType.getName());
	
	            String methodKey = methodKeyBuilder.toString();
	            Class<?> methodClass = method.getDeclaringClass();
	            Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
	            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
	                // Only add if not already found in a sub class
	                return true;
	            } else {
	                // Revert the put, old class is further down the class hierarchy
	                subscriberClassByMethodKey.put(methodKey, methodClassOld);
	                return false;
	            }
	        }
兜兜绕绕了一小圈。checkAdd方法返回的是true.
提问2 ：这里的checkAdd方法返回的都是true,那这个方法的作用是干嘛的？



回到前面
`findSubscriberMethods`方法的是`findUsingInfo`方法,`getSubscriberInfo`方法已经获取所有的Subscriber枚举的方法，并封装成`SubscriberMethod`存入集合。

	private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
	        FindState findState = prepareFindState();
			//初始化里面的值
	        findState.initForSubscriber(subscriberClass);
			//findState.clazz就是subscriberClass
	        while (findState.clazz != null) {
				//获取subscriberInfo对象
	            findState.subscriberInfo = getSubscriberInfo(findState);
	            if (findState.subscriberInfo != null) {
	                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
	                for (SubscriberMethod subscriberMethod : array) {
	                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
	                        findState.subscriberMethods.add(subscriberMethod);
	                    }
	                }
	            } else {
	                findUsingReflectionInSingleClass(findState);
	            }
	            findState.moveToSuperclass();
	        }
	        return getMethodsAndRelease(findState);
	    }
`moveToSuperclass` 方法，因为`skipSuperClasses`为false,所以走else,把里面的`clazz`类型设置成它的父类类型。因为后面又把`FinsState`里面的属性释放了。所以这里调用这个方法意义也不大。

	 void moveToSuperclass() {
	            if (skipSuperClasses) {
	                clazz = null;
	            } else {
	                clazz = clazz.getSuperclass();
	                String clazzName = clazz.getName();
	                /** Skip system classes, this just degrades performance. */
	                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
	                    clazz = null;
	                }
	            }
	        }
最后把`FindState`里面的`subscriberMethods`集合复制一个新集合。把`FindState`对象里面的属性全部释放资源。并且把`FindState`对象放入`POOL_SIZE`池中

	 private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
	        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
	        findState.recycle();
	        synchronized (FIND_STATE_POOL) {
	            for (int i = 0; i < POOL_SIZE; i++) {
	                if (FIND_STATE_POOL[i] == null) {
	                    FIND_STATE_POOL[i] = findState;
	                    break;
	                }
	            }
	        }
	        return subscriberMethods;
	    }

继续往前面看register方法。前面分析findSubscriberMethods方法已经找到了该类里面所有的`Subscriber`枚举标记的方法，并封装成SubscriberMethod对象。接下来应该就是该方法和该对象绑定在一起

	 public void register(Object subscriber) {
			//获取对象类型
	        Class<?> subscriberClass = subscriber.getClass();
			//找到类里面标记Subscribe枚举的方法，并封装成SubscriberMethod对象
	        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);
	        synchronized (this) {
				//遍历所有SubscriberMethod
	            for (SubscriberMethod subscriberMethod : subscriberMethods) {
	                subscribe(subscriber, subscriberMethod);
	            }
	        }
	    }

查看`subscribe`方法，把要执行的方法和订阅对象封装成Subscription对象。根据订阅的方法的参数类型，把所有参数方法一样的Subscription对象放入subscriptionsByEventType map集合中。

	 private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
	        Class<?> eventType = subscriberMethod.eventType;
			//把订阅者和执行方法封装到Subscription对象
	        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
			//根据参数类型从subscriptionsByEventType集合中获取Subscription集合
	        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
			
			//如果还没有。新创建一个。如果有。在后面根据优先级别放入对应的位置。
	        if (subscriptions == null) {
	            subscriptions = new CopyOnWriteArrayList<>();
	            subscriptionsByEventType.put(eventType, subscriptions);
	        } else {
				//如果register方法调用2次。则会抛此异常。
	            if (subscriptions.contains(newSubscription)) {
	                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
	                        + eventType);
	            }
	        }
	
	        int size = subscriptions.size();
			//根据priority。把优先级别高的放入前面
	        for (int i = 0; i <= size; i++) {
	            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
	                subscriptions.add(i, newSubscription);
	                break;
	            }
	        }
	
	        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
	        if (subscribedEvents == null) {
	            subscribedEvents = new ArrayList<>();
	            typesBySubscriber.put(subscriber, subscribedEvents);
	        }
	        subscribedEvents.add(eventType);
			//默认的这个值为false
	        if (subscriberMethod.sticky) {
	            if (eventInheritance) {
	                // Existing sticky events of all subclasses of eventType have to be considered.
	                // Note: Iterating over all events may be inefficient with lots of sticky events,
	                // thus data structure should be changed to allow a more efficient lookup
	                // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
	                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
	                for (Map.Entry<Class<?>, Object> entry : entries) {
	                    Class<?> candidateEventType = entry.getKey();
	                    if (eventType.isAssignableFrom(candidateEventType)) {
	                        Object stickyEvent = entry.getValue();
	                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
	                    }
	                }
	            } else {
					//由于还没有往stickyEvents集合里面放入东西。故这里获取是个null
	                Object stickyEvent = stickyEvents.get(eventType);
	                checkPostStickyEventToSubscription(newSubscription, stickyEvent);
	            }
	        }
	    }

checkPostStickyEventToSubscription调用的是postToSubscription方法,参数stichyEvent为null.则执行完毕

	 private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
	        if (stickyEvent != null) {
	            // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
	            // --> Strange corner case, which we don't take care of here.
	            postToSubscription(newSubscription, stickyEvent, Looper.getMainLooper() == Looper.myLooper());
	        }
	    }


register方法 小结 ：根据当前订阅者的类型去METHOD_CACHE集合中查找是否有缓存。没有获取当前类型的所有的订阅方法，封装到SubscriberMethod中。根据事件类型一样的和优先级别排好序放入List集合中，把List集合根据事件类型放入subscriptionsByEventType map集合中。这样下次接收到事件时就可以根据事件类型从subscriptionsByEventType中获取所有的方法，依次执行。


再看post方法



	public void post(Object event) {
			//获取当前线程的PostingThreadState对象
	        PostingThreadState postingState = currentPostingThreadState.get();
	        List<Object> eventQueue = postingState.eventQueue;
			//添加到事件队列中
	        eventQueue.add(event);
			//默认为false
	        if (!postingState.isPosting) {
	            postingState.isMainThread = Looper.getMainLooper() == Looper.myLooper();
	            postingState.isPosting = true;
	            if (postingState.canceled) {
	                throw new EventBusException("Internal error. Abort state was not reset");
	            }
	            try {
	                while (!eventQueue.isEmpty()) {
						//获取第一个事件，
	                    postSingleEvent(eventQueue.remove(0), postingState);
	                }
	            } finally {
	                postingState.isPosting = false;
	                postingState.isMainThread = false;
	            }
	        }
	    }


	  private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
	        Class<?> eventClass = event.getClass();
	        boolean subscriptionFound = false;
			
			//默认是个true
	        if (eventInheritance) {
				//根据当前事件类型，一直往上查找父类型，以及接口。这样订阅了该事件的父类或者实现的接口也能接收到该事件
	            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
	            int countTypes = eventTypes.size();
	            for (int h = 0; h < countTypes; h++) {
	                Class<?> clazz = eventTypes.get(h);
	                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
	            }
	        } else {
	            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
	        }
	        if (!subscriptionFound) {
	            if (logNoSubscriberMessages) {
	                Log.d(TAG, "No subscribers registered for event " + eventClass);
	            }
	            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
	                    eventClass != SubscriberExceptionEvent.class) {
	                post(new NoSubscriberEvent(this, event));
	            }
	        }
	    }


    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
			//根据消息类型，查找所有订阅了该类型的方法
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
					//执行每个订阅的方法
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }

postToSubscription方法就是把要执行的放入哪个线程去执行。默认是ThreadMode.POSTING,也就是发送消息的方法在哪个线程。接收消息的方法就在哪个线程。Main就是接收方法的在主线程。BACKROUND和ASYNC都是在后台线程。

	 private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
	        switch (subscription.subscriberMethod.threadMode) {
	            case POSTING:
	                invokeSubscriber(subscription, event);
	                break;
	            case MAIN:
	                if (isMainThread) {
	                    invokeSubscriber(subscription, event);
	                } else {
	                    mainThreadPoster.enqueue(subscription, event);
	                }
	                break;
	            case BACKGROUND:
	                if (isMainThread) {
	                    backgroundPoster.enqueue(subscription, event);
	                } else {
	                    invokeSubscriber(subscription, event);
	                }
	                break;
	            case ASYNC:
	                asyncPoster.enqueue(subscription, event);
	                break;
	            default:
	                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
	        }
	    }
不管前台后台的，都会调用这个方法

	void invokeSubscriber(Subscription subscription, Object event) {
	        try {
	            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
	        } catch (InvocationTargetException e) {
	            handleSubscriberException(subscription, event, e.getCause());
	        } catch (IllegalAccessException e) {
	            throw new IllegalStateException("Unexpected exception", e);
	        }
	    }


post方法小结：前面register方法已经把要执行的方法以及要执行方法的顺序都封装好，并且根据事件类型存入subscriptionsByEventType集合中了。而post要执行的就是根据事件类型，把对应的要执行的方法获取一一执行。这里要注意的就是，订阅了该事件类型的父类型，以及接口也都会接收到该事件。