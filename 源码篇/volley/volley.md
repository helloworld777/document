
使用
	
	//生成一个请求队列
	RequestQueue mRequestQueue = Volley.newRequestQueue(mContext);
	//生成一个请求
	StringRequest strRequest = new StringRequest(Method.POST, url
		//请求成功的回调
		,new Listener<String>() {
			@Override
			public void onResponse(String paramT) {
			}
			//请求失败的回调
		}, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
			}
		};
	// 将请求加入队列
	mRequestQueue.add(req);


newRequestQueue方法最终会调用带参数的newRequestQueue(Context context, HttpStack stack, int maxDiskCacheBytes)

	public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
	        return newRequestQueue(context, stack, -1);
	    }
	
	    public static RequestQueue newRequestQueue(Context context) {
	        return newRequestQueue(context, (HttpStack)null);
	    }

Volley newRequestQueue方法

	 public static RequestQueue newRequestQueue(Context context, HttpStack stack, int maxDiskCacheBytes) {
	        File cacheDir = new File(context.getCacheDir(), "volley");
	        String userAgent = "volley/0";
	
	        try {
	            String packageName = context.getPackageName();
	            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
	            userAgent = packageName + "/" + info.versionCode;
	        } catch (NameNotFoundException var7) {
	            ;
	        }
	
	        if (stack == null) {
				//用HttpURLConnection去连接
	            if (VERSION.SDK_INT >= 9) {
	                stack = new HurlStack();
	            } else {
					//用HttpClient去连接
	                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
	            }
	        }
	
	        Network network = new BasicNetwork((HttpStack)stack);
	        RequestQueue queue;
	        if (maxDiskCacheBytes <= -1) {
				//走这里
	            queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
	        } else {
	            queue = new RequestQueue(new DiskBasedCache(cacheDir, maxDiskCacheBytes), network);
	        }
			
			//初始化后调用队列的start方法
	        queue.start();
	        return queue;
	    }

//初始化里面的属性

	 public RequestQueue(Cache cache, Network network, int threadPoolSize, ResponseDelivery delivery) {
	        this.mSequenceGenerator = new AtomicInteger();
	        this.mWaitingRequests = new HashMap();
	        this.mCurrentRequests = new HashSet();
	        this.mCacheQueue = new PriorityBlockingQueue();
	        this.mNetworkQueue = new PriorityBlockingQueue();
	        this.mFinishedListeners = new ArrayList();
	        this.mCache = cache;
	        this.mNetwork = network;
	        this.mDispatchers = new NetworkDispatcher[threadPoolSize];
	        this.mDelivery = delivery;
	    }
	
	    public RequestQueue(Cache cache, Network network, int threadPoolSize) {
	        this(cache, network, threadPoolSize, new ExecutorDelivery(new Handler(Looper.getMainLooper())));
	    }
	
	    public RequestQueue(Cache cache, Network network) {
	        this(cache, network, 4);
	    }


初始化后调用队列的start方法

	public void start() {
        this.stop();
		//把两个队列传入缓存线程
        this.mCacheDispatcher = new CacheDispatcher(this.mCacheQueue, this.mNetworkQueue, this.mCache, this.mDelivery);
        this.mCacheDispatcher.start();
		//创建4个网络线程
        for(int i = 0; i < this.mDispatchers.length; ++i) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(this.mNetworkQueue, this.mNetwork, this.mCache, this.mDelivery);
            this.mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }

    }
	//如果里面有线程。先把里面的线程stop
	public void stop() {
        if (this.mCacheDispatcher != null) {
            this.mCacheDispatcher.quit();
        }

        for(int i = 0; i < this.mDispatchers.length; ++i) {
            if (this.mDispatchers[i] != null) {
                this.mDispatchers[i].quit();
            }
        }
    }

小结 Volley.newRequestQueue(mContext) 方法生成一个RequestQueue对象，该对象里面有两个阻塞队列，一个是缓存队列，一个是网络队列。然后开启了一个缓存线程，4个网络线程。

查看缓存线程的run方法

	 public void run() {
	        if (DEBUG) {
	            VolleyLog.v("start new dispatcher", new Object[0]);
	        }
			//设置线程的级别
	        Process.setThreadPriority(10);
			//调用mCache初始化方法。这个mCache是从外面传进来的。是个DiskBasedCache类型
	        this.mCache.initialize();
			//一个无限循环
	        while(true) {
	            final Request request;
				//还有一个无限循环，这个循环是从缓存队列里面获取请求对象。拿到则退出
	            while(true) {
	                request = null;
	
	                try {
	                    request = (Request)this.mCacheQueue.take();
	                    break;
	                } catch (InterruptedException var6) {
	                    if (this.mQuit) {
	                        return;
	                    }
	                }
	            }
	
	            try {
	                request.addMarker("cache-queue-take");
					//如果已经取消了。则调用finished方法
	                if (request.isCanceled()) {
	                    request.finish("cache-discard-canceled");
	                } else {
						//从缓存中拿取Entry对象
	                    Entry entry = this.mCache.get(request.getCacheKey());
						//如果没有，放入网络队列里面
	                    if (entry == null) {
	                        request.addMarker("cache-miss");
	                        this.mNetworkQueue.put(request);
						////如果已经过期，放入网络队列里面
	                    } else if (entry.isExpired()) {
	                        request.addMarker("cache-hit-expired");
	                        request.setCacheEntry(entry);
	                        this.mNetworkQueue.put(request);
	                    } else {
							//如果有缓存，并且没有过期。直接解析，至于怎么解析，看什么类型的请求就解析成什么，这方法放在请求子类里面实现
	                        request.addMarker("cache-hit");
	                        Response<?> response = request.parseNetworkResponse(new NetworkResponse(entry.data, entry.responseHeaders));
	                        request.addMarker("cache-hit-parsed");
							//如果不需要刷新。则直接回调回去。需要刷新，继续放到网络队列里面
	                        if (!entry.refreshNeeded()) {
	                            this.mDelivery.postResponse(request, response);
	                        } else {
	                            request.addMarker("cache-hit-refresh-needed");
	                            request.setCacheEntry(entry);
	                            response.intermediate = true;
	                            this.mDelivery.postResponse(request, response, new Runnable() {
	                                public void run() {
	                                    try {
	                                        CacheDispatcher.this.mNetworkQueue.put(request);
	                                    } catch (InterruptedException var2) {
	                                        ;
	                                    }
	
	                                }
	                            });
	                        }
	                    }
	                }
	            } catch (Exception var5) {
	                VolleyLog.e(var5, "Unhandled exception %s", new Object[]{var5.toString()});
	            }
	        }
	    }

从上面代码可知，先是把当前缓存线程级别设置成10. 就是Process类里面后台线程的级别。

	 /**
	     * Standard priority background threads.  This gives your thread a slightly
	     * lower than normal priority, so that it will have less chance of impacting
	     * the responsiveness of the user interface.
	     * Use with {@link #setThreadPriority(int)} and
	     * {@link #setThreadPriority(int, int)}, <b>not</b> with the normal
	     * {@link java.lang.Thread} class.
	     */
	    public static final int THREAD_PRIORITY_BACKGROUND = 10;

然后初始化调用缓存类mCache的初始化方法。之后就while无限循环中。后面获取缓存队列中的请求时也是用一个while无限循环。只不过这个循环获取到一个请求后就会退出。
>至于这里为什么获取请求的时候要加个while循环？  
>请求队列请求不到的时候不是自动就会阻塞在那里么？

继续往后看  
 >1 如果该请求已经取消了。则调用Request的finish方法。  
 2 如果缓存中没有该请求的数据，则直接把该请求放入网络队列里面  
 3 如果缓存中有，则判断是否过期，如果过期了，则放入网络列队  
 4 如果没有过期，判断是否要刷新。如果不要刷新。则把数据回调给监听  
 5 如果要刷新，先回调数据给监听，然后再把请求放入网络队列  


再看网络线程NetworkDispatcher的run方法


	 public void run() {
			//也是设置线程级别
	        Process.setThreadPriority(10);
			//也是无限循环
	        while(true) {
	            Request request;
	            long startTimeMs;
				//无限循环。从网络列表里面获取。如果成功。则退出
	            while(true) {
	                startTimeMs = SystemClock.elapsedRealtime();
	                request = null;
	
	                try {
	                    request = (Request)this.mQueue.take();
	                    break;
	                } catch (InterruptedException var6) {
	                    if (this.mQuit) {
	                        return;
	                    }
	                }
	            }
	
	            try {
	                request.addMarker("network-queue-take");
					//1 如果已经取消了。调用finish方法
	                if (request.isCanceled()) {
	                    request.finish("network-discard-cancelled");
	                } else {
						//这方法干哈用的也不清楚，不过不影响后面流程分析，略过
	                    this.addTrafficStatsTag(request);
						//2 调用Network 去服务器拿数据了。
	                    NetworkResponse networkResponse = this.mNetwork.performRequest(request);
	                    request.addMarker("network-http-complete");
	                    if (networkResponse.notModified && request.hasHadResponseDelivered()) {
	                        request.finish("not-modified");
	                    } else {
							//解析数据
	                        Response<?> response = request.parseNetworkResponse(networkResponse);
	                        request.addMarker("network-parse-complete");
							//如果要缓存，则把数据缓存
	                        if (request.shouldCache() && response.cacheEntry != null) {
	                            this.mCache.put(request.getCacheKey(), response.cacheEntry);
	                            request.addMarker("network-cache-written");
	                        }
							//调用这个方法后,hasHadResponseDelivered方法返回的就是true
	                        request.markDelivered();
							//回调给监听者
	                        this.mDelivery.postResponse(request, response);
	                    }
	                }
	            } catch (VolleyError var7) {
	                var7.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
	                this.parseAndDeliverNetworkError(request, var7);
	            } catch (Exception var8) {
	                VolleyLog.e(var8, "Unhandled exception %s", new Object[]{var8.toString()});
	                VolleyError volleyError = new VolleyError(var8);
	                volleyError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
	                this.mDelivery.postError(request, volleyError);
	            }
	        }
	    }

也是开启无限循环，从网络队列里面获取请求。调用传进来的mNetwork处理网络请求，并调用Request 自己的解析返回数据方法，返回需要的数据封装到Respose里面，并且回调给监听者

查看怎么处理网络请求的，这个mNetwork 就是前面Volley创建好传进来的
	
		 	if (stack == null) {
	            if (VERSION.SDK_INT >= 9) {
	                stack = new HurlStack();
	            } else {
	                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
	            }
	        }
	
	        Network network = new BasicNetwork((HttpStack)stack);
	        RequestQueue queue;
	        if (maxDiskCacheBytes <= -1) {
	            queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
	        } else {
	            queue = new RequestQueue(new DiskBasedCache(cacheDir, maxDiskCacheBytes), network);
	        }

看 HurlStack  的 performRequest

	 public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
	        String url = request.getUrl();
	        HashMap<String, String> map = new HashMap();
	        map.putAll(request.getHeaders());
	        map.putAll(additionalHeaders);
	        if (this.mUrlRewriter != null) {
	            String rewritten = this.mUrlRewriter.rewriteUrl(url);
	            if (rewritten == null) {
	                throw new IOException("URL blocked by rewriter: " + url);
	            }
	
	            url = rewritten;
	        }
	
	        URL parsedUrl = new URL(url);
			//1 打开连接
	        HttpURLConnection connection = this.openConnection(parsedUrl, request);
	        Iterator var8 = map.keySet().iterator();
			
			//2 设置头信息
	        while(var8.hasNext()) {
	            String headerName = (String)var8.next();
	            connection.addRequestProperty(headerName, (String)map.get(headerName));
	        }
			//3设置请求方式。POST或者GEIT或者其他
	        setConnectionParametersForRequest(connection, request);
	        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
	        int responseCode = connection.getResponseCode();
			//4 获取请求码
	        if (responseCode == -1) {
	            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
	        } else {
				//把版本协议，返回码，返回message封装到StatusLine对象
	            StatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
				//把前面的对象又封装到BasicHttpResponse对象
	            BasicHttpResponse response = new BasicHttpResponse(responseStatus);
				//如果有body数据
	            if (hasResponseBody(request.getMethod(), responseStatus.getStatusCode())) {
					//把返回的body信息放入respose。这里放入的还是一个输入流。还没解析输入流里面的数据
	                response.setEntity(entityFromConnection(connection));
	            }
	
	            Iterator var12 = connection.getHeaderFields().entrySet().iterator();
	
	            while(var12.hasNext()) {
	                Entry<String, List<String>> header = (Entry)var12.next();
	                if (header.getKey() != null) {
	                    Header h = new BasicHeader((String)header.getKey(), (String)((List)header.getValue()).get(0));
	                    response.addHeader(h);
	                }
	            }
	
	            return response;
	        }
	    }

	//注释1 打开连接，设置连接超时，读取超时等信息
	private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
	        HttpURLConnection connection = this.createConnection(url);
	        int timeoutMs = request.getTimeoutMs();
	        connection.setConnectTimeout(timeoutMs);
	        connection.setReadTimeout(timeoutMs);
	        connection.setUseCaches(false);
	        connection.setDoInput(true);
	        if ("https".equals(url.getProtocol()) && this.mSslSocketFactory != null) {
	            ((HttpsURLConnection)connection).setSSLSocketFactory(this.mSslSocketFactory);
	        }
	
	        return connection;
	    }
	//注释3 设置各种请求方法。不过一般就用到了GET 和POST
	 static void setConnectionParametersForRequest(HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
        switch(request.getMethod()) {
        case -1:
            byte[] postBody = request.getPostBody();
            if (postBody != null) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.addRequestProperty("Content-Type", request.getPostBodyContentType());
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.write(postBody);
                out.close();
            }
            break;
        case 0:
            connection.setRequestMethod("GET");
            break;
        case 1:
            connection.setRequestMethod("POST");
            addBodyIfExists(connection, request);
            break;
        ...
        default:
            throw new IllegalStateException("Unknown method type.");
        }

    }
	//如果有body的。把body设置过去
	 private static void addBodyIfExists(HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }

    }



HurlStack  的 performRequest返回的HttpResponse对象就是服务器的原始数据，甚至是直接把连接输入流也直接丢给调用者，这时候BasicNetwork又对HttpStack做了一层封装。
	
	 	if (stack == null) {
	            if (VERSION.SDK_INT >= 9) {
	                stack = new HurlStack();
	            } else {
	                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
	            }
	        }
	
	        Network network = new BasicNetwork((HttpStack)stack);

看BasicNetwork的performRequest方法,根据不同的返回码做不同处理，包装成NetworkResponse对象返回


	 public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        long requestStart = SystemClock.elapsedRealtime();

        while(true) {
            HttpResponse httpResponse = null;
            byte[] responseContents = null;
            Map responseHeaders = Collections.emptyMap();

            try {
                Map<String, String> headers = new HashMap();
                this.addCacheHeaders(headers, request.getCacheEntry());
                httpResponse = this.mHttpStack.performRequest(request, headers);
                StatusLine statusLine = httpResponse.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                responseHeaders = convertHeaders(httpResponse.getAllHeaders());
				
				//根据返回码做不同的处理
                if (statusCode == 304) {
                    Entry entry = request.getCacheEntry();
                    if (entry == null) {
                        return new NetworkResponse(304, (byte[])null, responseHeaders, true, SystemClock.elapsedRealtime() - requestStart);
                    }

                    entry.responseHeaders.putAll(responseHeaders);
                    return new NetworkResponse(304, entry.data, entry.responseHeaders, true, SystemClock.elapsedRealtime() - requestStart);
                }

                if (statusCode == 301 || statusCode == 302) {
                    String newUrl = (String)responseHeaders.get("Location");
                    request.setRedirectUrl(newUrl);
                }

                byte[] responseContents;
                if (httpResponse.getEntity() != null) {
                    responseContents = this.entityToBytes(httpResponse.getEntity());
                } else {
                    responseContents = new byte[0];
                }

                long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
                this.logSlowRequests(requestLifetime, request, responseContents, statusLine);
                if (statusCode >= 200 && statusCode <= 299) {
                    return new NetworkResponse(statusCode, responseContents, responseHeaders, false, SystemClock.elapsedRealtime() - requestStart);
                }

                throw new IOException();
            } 
			...
        }
    }


再看网络线程里面的run 方法

	 	public void run() {
				//也是设置线程级别
		        Process.setThreadPriority(10);
				//也是无限循环
		        while(true) {
		           ...
		            try {
		                request.addMarker("network-queue-take");
						...
							//2 调用Network 去服务器拿数据了。
		                    NetworkResponse networkResponse = this.mNetwork.performRequest(request);
		                    request.addMarker("network-http-complete");
		                    if (networkResponse.notModified && request.hasHadResponseDelivered()) {
		                        request.finish("not-modified");
		                    } else {
								//解析数据
		                        Response<?> response = request.parseNetworkResponse(networkResponse);
		                        request.addMarker("network-parse-complete");
								//如果要缓存，则把数据缓存
		                        if (request.shouldCache() && response.cacheEntry != null) {
		                            this.mCache.put(request.getCacheKey(), response.cacheEntry);
		                            request.addMarker("network-cache-written");
		                        }
								//调用这个方法后,hasHadResponseDelivered方法返回的就是true
		                        request.markDelivered();
								//回调给监听者
		                        this.mDelivery.postResponse(request, response);
		                 ...
		    }

我们传入的是StringRequest。返回的就是解析后的String

	public class StringRequest extends Request<String> {
	    protected Response<String> parseNetworkResponse(NetworkResponse response) {
	        String parsed;
	        try {
	            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
	        } catch (UnsupportedEncodingException var4) {
	            parsed = new String(response.data);
	        }
			//返回解析的后数据，和要缓存的数据
	        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
	    }
	}

小结：当调用newRequestQueue方法后，就有1个缓存线程，4个网络线程一直在运行，等待请求处理。

官网的工作流程图

![](https://i.imgur.com/j2uNsnM.png)