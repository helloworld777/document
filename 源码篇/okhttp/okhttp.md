#Okhttp流程解读
		
调用代码

	 	//1生成一个okhttpclient对象
	  OkHttpClient client = new OkHttpClient();
		//2利用url生成一个请求
	  Request request = new Request.Builder().url(url).build();
		//3生成一个Call对象
	  final Call call = client.newCall(request);
		//4从服务器获取返回结果
		call.enqueue(new Callback() {
		    @Override
		   public void onFailure(Call call, final IOException e) {
		
		    }
		    @Override
		    public void onResponse(Call call, final Response response) throws IOException {
		                
		       }
		  });


	//生成一个默认的builder
	 public OkHttpClient() {
	    this(new Builder());
	  }
	//初始化里面的对象
	 public Builder() {
      dispatcher = new Dispatcher();
      protocols = DEFAULT_PROTOCOLS;
      connectionSpecs = DEFAULT_CONNECTION_SPECS;
      eventListenerFactory = EventListener.factory(EventListener.NONE);
      proxySelector = ProxySelector.getDefault();
      cookieJar = CookieJar.NO_COOKIES;
      socketFactory = SocketFactory.getDefault();
      hostnameVerifier = OkHostnameVerifier.INSTANCE;
      certificatePinner = CertificatePinner.DEFAULT;
      proxyAuthenticator = Authenticator.NONE;
      authenticator = Authenticator.NONE;
      connectionPool = new ConnectionPool();
      dns = Dns.SYSTEM;
      followSslRedirects = true;
      followRedirects = true;
      retryOnConnectionFailure = true;
      connectTimeout = 10_000;
      readTimeout = 10_000;
      writeTimeout = 10_000;
      pingInterval = 0;
    }

`Request`对象生成也是构造者模式

	//默认GET方法
	 public Builder() {
      this.method = "GET";
      this.headers = new Headers.Builder();
    }
	//封装成url 封装到HttpUrl
	 public Builder url(String url) {
      if (url == null) throw new NullPointerException("url == null");

      // Silently replace web socket URLs with HTTP URLs.
      if (url.regionMatches(true, 0, "ws:", 0, 3)) {
        url = "http:" + url.substring(3);
      } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
        url = "https:" + url.substring(4);
      }

      HttpUrl parsed = HttpUrl.parse(url);
      if (parsed == null) throw new IllegalArgumentException("unexpected url: " + url);
      return url(parsed);
    }

	//生成一个RealCall对象
	@Override 
	public Call newCall(Request request) {
	    return new RealCall(this, request, false /* for web socket */);
	  }


`RealCall` 里面的 `enqueue`方法，调用的`OkHttpClien`t里面的`Dispatcher`对象方法


	   @Override 
	public void enqueue(Callback responseCallback) {
	    synchronized (this) {
	      if (executed) throw new IllegalStateException("Already Executed");
	      executed = true;
	    }
	    captureCallStackTrace();
		//封装到AsyncCall里面
	    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  	}


`Dispatcher` 的`enqueued`方法，`runningSyncCalls`是个`ArrayDeque`集合。是个双端列队。
	
	 synchronized void enqueue(AsyncCall call) {
		//如果集合大小小于64
	    if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
		//添加到集合
	      runningAsyncCalls.add(call);
		//放到线程池执行
	      executorService().execute(call);
	    } else {
	      readyAsyncCalls.add(call);
	    }
 	 }
	
`AsyncCall`继承了`NamedRunnable`类，`NamedRunnable`类实现了`Runnable`接口，并且在`run`方法里面会调用`execute`方法。因此看`execute`方法

	final class AsyncCall extends NamedRunnable {
    

    @Override 
	protected void execute() {
	      boolean signalledCallback = false;
	      try {
			//调用了getResponseWithInterceptorChain方法。
	        Response response = getResponseWithInterceptorChain();
	        if (retryAndFollowUpInterceptor.isCanceled()) {
	          signalledCallback = true;
	          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
	        } else {
	          signalledCallback = true;
	          responseCallback.onResponse(RealCall.this, response);
	        }
	      } catch (IOException e) {
	        if (signalledCallback) {
	          // Do not signal the callback twice!
	          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
	        } else {
	          responseCallback.onFailure(RealCall.this, e);
	        }
	      } finally {
	        client.dispatcher().finished(this);
	      }
	    }
	}

调用了`getResponseWithInterceptorChain`方法，`getResponseWithInterceptorChain`里面添加了拦截器。然后调用`RealInterceptorChain的proceed`方法，



	Response getResponseWithInterceptorChain() throws IOException {
	    // Build a full stack of interceptors.
	    List<Interceptor> interceptors = new ArrayList<>();
	    interceptors.addAll(client.interceptors());
	    interceptors.add(retryAndFollowUpInterceptor);
	    interceptors.add(new BridgeInterceptor(client.cookieJar()));
	    interceptors.add(new CacheInterceptor(client.internalCache()));
	    interceptors.add(new ConnectInterceptor(client));
	    if (!forWebSocket) {
	      interceptors.addAll(client.networkInterceptors());
	    }
	    interceptors.add(new CallServerInterceptor(forWebSocket));
	
	    Interceptor.Chain chain = new RealInterceptorChain(
	        interceptors, null, null, null, 0, originalRequest);
	    return chain.proceed(originalRequest);
	  }

	public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
	      RealConnection connection) throws IOException {
		    if (index >= interceptors.size()) throw new AssertionError();
		
		    calls++;
		
		  ...
		    RealInterceptorChain next = new RealInterceptorChain(
		        interceptors, streamAllocation, httpCodec, connection, index + 1, request);
		    Interceptor interceptor = interceptors.get(index);
			//执行拦截器里面的方法
		    Response response = interceptor.intercept(next);
		...
	
	    return response;
  	}


因为我们自己没添加拦截器。而`getResponseWithInterceptorChain`方法里面添加了很多个拦截器，拦截器会依次一个一个执行。

此时这个`index=0`，首先执行的是第一个也就是`retryAndFollowUpInterceptor的intercept`方法 ,方法一点长，我们暂时省略流程无关。这里调用了传进来的`Chain` 对象的`proceed`方法

	@Override 
	public Response intercept(Chain chain) throws IOException {
	    Request request = chain.request();
	    streamAllocation = new StreamAllocation(
	        client.connectionPool(), createAddress(request.url()), callStackTrace);
	    int followUpCount = 0;
	    Response priorResponse = null;
	    while (true) {
	      ...
	      Response response = null;
	      boolean releaseConnection = true;
	      try {
			//执行了传进来的对象的proceed方法
	        response = ((RealInterceptorChain) chain).proceed(request, streamAllocation, null, null);
	        releaseConnection = false;
	     ...
	    }
	  }

	

又到这里了，注意。这个时候`index=1`了。又生成了`RealInterceptorChain`对象。并且传入`index+1`。

	public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
		      RealConnection connection) throws IOException {
			    if (index >= interceptors.size()) throw new AssertionError();
			
			    calls++;
			
			  ...
			    RealInterceptorChain next = new RealInterceptorChain(
			        interceptors, streamAllocation, httpCodec, connection, index + 1, request);
			    Interceptor interceptor = interceptors.get(index);
				//执行拦截器里面的方法
			    Response response = interceptor.intercept(next);
			...
		
		    return response;
	  	}

再看前面添加拦截器的顺序。这个时候是`BridgeInterceptor`拦截器对象

 		interceptors.addAll(client.interceptors());
	    interceptors.add(retryAndFollowUpInterceptor);
	    interceptors.add(new BridgeInterceptor(client.cookieJar()));
	    interceptors.add(new CacheInterceptor(client.internalCache()));
	    interceptors.add(new ConnectInterceptor(client));
	    if (!forWebSocket) {
	      interceptors.addAll(client.networkInterceptors());
	    }
	    interceptors.add(new CallServerInterceptor(forWebSocket));

看`BridgeInterceptor`的`intercept`方法，这个主要是设置了请求的头部信息,继续执行后面的拦截器方法。

	@Override 
	public Response intercept(Chain chain) throws IOException {
	    Request userRequest = chain.request();
	    Request.Builder requestBuilder = userRequest.newBuilder();
	
	    RequestBody body = userRequest.body();
	    if (body != null) {
	      MediaType contentType = body.contentType();
	      if (contentType != null) {
	        requestBuilder.header("Content-Type", contentType.toString());
	      }
	
	      long contentLength = body.contentLength();
	      if (contentLength != -1) {
	        requestBuilder.header("Content-Length", Long.toString(contentLength));
	        requestBuilder.removeHeader("Transfer-Encoding");
	      } else {
	        requestBuilder.header("Transfer-Encoding", "chunked");
	        requestBuilder.removeHeader("Content-Length");
	      }
	    }
	
	    if (userRequest.header("Host") == null) {
	      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
	    }
	
	    if (userRequest.header("Connection") == null) {
	      requestBuilder.header("Connection", "Keep-Alive");
	    }
	
	    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
	    // the transfer stream.
	    boolean transparentGzip = false;
	    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
	      transparentGzip = true;
	      requestBuilder.header("Accept-Encoding", "gzip");
	    }
	
	    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
	    if (!cookies.isEmpty()) {
	      requestBuilder.header("Cookie", cookieHeader(cookies));
	    }
	
	    if (userRequest.header("User-Agent") == null) {
	      requestBuilder.header("User-Agent", Version.userAgent());
	    }
	
	    Response networkResponse = chain.proceed(requestBuilder.build());
	
	    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());
	
	    Response.Builder responseBuilder = networkResponse.newBuilder()
	        .request(userRequest);
	
	    if (transparentGzip
	        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
	        && HttpHeaders.hasBody(networkResponse)) {
	      GzipSource responseBody = new GzipSource(networkResponse.body().source());
	      Headers strippedHeaders = networkResponse.headers().newBuilder()
	          .removeAll("Content-Encoding")
	          .removeAll("Content-Length")
	          .build();
	      responseBuilder.headers(strippedHeaders);
	      responseBuilder.body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)));
	    }
	
	    return responseBuilder.build();
	  }


现在是`CacheInterceptor`的`intercept`，这里的请求是直接调用后面拦截器处理方法。继续看后面的拦截器。

	  @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        okhttp3.Response originalResponse = chain.proceed(chain.request());
        String cacheControl = originalResponse.header("Cache-Control");
        //String cacheControl = request.cacheControl().toString();
        LogWraper.d("Novate", maxStaleOnline + "s load cache:" + cacheControl);
        if (TextUtils.isEmpty(cacheControl) || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                cacheControl.contains("must-revalidate") || cacheControl.contains("max-age") || cacheControl.contains("max-stale")) {
            return originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, max-age=" + maxStale)
                    .build();

        } else {
            return originalResponse;
        }
    }

`ConnectInterceptor`的`intercept`
	
	@Override 
	public Response intercept(Chain chain) throws IOException {
	    RealInterceptorChain realChain = (RealInterceptorChain) chain;
	    Request request = realChain.request();
	    StreamAllocation streamAllocation = realChain.streamAllocation();
	
	    // We need the network to satisfy this request. Possibly for validating a conditional GET.
	    boolean doExtensiveHealthChecks = !request.method().equals("GET");
	    HttpCodec httpCodec = streamAllocation.newStream(client, doExtensiveHealthChecks);
	    RealConnection connection = streamAllocation.connection();
	
	    return realChain.proceed(request, streamAllocation, httpCodec, connection);
	  }

在`streamAllocation`对象的`newStream`方法里面建起和服务器的连接，`newStream`方法调用了`findHealthyConnection`方法，返回的是`Http1Codec`或者`Http2Codec`对象。对应的就是`HTTP/1.1`或者`HTTP/2`协议

	//返回的是Http1Codec或者Http2Codec
	public HttpCodec newStream(OkHttpClient client, boolean doExtensiveHealthChecks) {
	    ....
	
	    try {
	      RealConnection resultConnection = findHealthyConnection(connectTimeout, readTimeout,
	          writeTimeout, connectionRetryEnabled, doExtensiveHealthChecks);
	     ...
	    } catch (IOException e) {
	      throw new RouteException(e);
	    }
	  }

现在主要看连接的部分。
`findHealthyConnection`方法调用了`findConnection`方法

	private RealConnection findHealthyConnection(int connectTimeout, int readTimeout,
	      int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks)
	      throws IOException {
	    while (true) {
	      RealConnection candidate = findConnection(connectTimeout, readTimeout, writeTimeout,
	          connectionRetryEnabled);
	
	      ...
	  }

`findConnection`方法会去连接池`connectionPool`中拿，没拿到。则去连接。连接的方法是通过`socket`去连接的。连接成功把数据读取，然后放入连接池。这样做到复用连接。



	  private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout,
	      boolean connectionRetryEnabled) throws IOException {
	    Route selectedRoute;
	    synchronized (connectionPool) {
	     ...
		
	      // Attempt to get a connection from the pool.
		//尝试从连接池中获取。有直接返回
	      Internal.instance.get(connectionPool, address, this, null);
	      if (connection != null) {
	        return connection;
	      }
	
	      selectedRoute = route;
	    }
	
	    //...new 一个
	      result = new RealConnection(connectionPool, selectedRoute);
	      acquire(result);

	    // Do TCP + TLS handshakes. This is a blocking operation.
		//去连接
	    result.connect(connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled);
	    routeDatabase().connected(result.route());
	
	    Socket socket = null;
	    synchronized (connectionPool) {
	      // Pool the connection.
			//把连接放入连接池
	      Internal.instance.put(connectionPool, result);
	      ...
	    }
	    closeQuietly(socket);
	    return result;
	  }

`connect`方法.省略了部分代码。最后都会调用`connectSocket`方法。利用`socket`去连接

	public void connect(
	      int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled) {
	   ...
	    while (true) {
	      try {
	        if (route.requiresTunnel()) {
	          connectTunnel(connectTimeout, readTimeout, writeTimeout);
	        } else {
	          connectSocket(connectTimeout, readTimeout);
	        }
	        establishProtocol(connectionSpecSelector);
	      ...
	  }

连接成功后，利用OkIO把数据读取

	private void connectSocket(int connectTimeout, int readTimeout) throws IOException {
	    Proxy proxy = route.proxy();
	    Address address = route.address();
	
	    rawSocket = proxy.type() == Proxy.Type.DIRECT || proxy.type() == Proxy.Type.HTTP
	        ? address.socketFactory().createSocket()
	        : new Socket(proxy);
	
	    rawSocket.setSoTimeout(readTimeout);
	    try {
	      Platform.get().connectSocket(rawSocket, route.socketAddress(), connectTimeout);
	    } catch (ConnectException e) {
	      ConnectException ce = new ConnectException("Failed to connect to " + route.socketAddress());
	      ce.initCause(e);
	      throw ce;
	    }
		....
	    try {
	      source = Okio.buffer(Okio.source(rawSocket));
	      sink = Okio.buffer(Okio.sink(rawSocket));
	    } catch (NullPointerException npe) {
	      if (NPE_THROW_WITH_NULL.equals(npe.getMessage())) {
	        throw new IOException(npe);
	      }
	    }
	  }

返回前面，`ConnectInterceptor`的`intercept`方法调用后，已经建立好连接，并且读取了数据，主要是的工作在`RealConnection`类中，并且还兼容了HTTP1和HTTP2协议。

继续看后面的拦截器。由于我们没有添加`networkInterceptor.`因此现在到`CallServerInterceptor`执行了。

		interceptors.addAll(client.interceptors());
	    interceptors.add(retryAndFollowUpInterceptor);
	    interceptors.add(new BridgeInterceptor(client.cookieJar()));
	    interceptors.add(new CacheInterceptor(client.internalCache()));
	    interceptors.add(new ConnectInterceptor(client));
	    if (!forWebSocket) {
	      interceptors.addAll(client.networkInterceptors());
	    }
	    interceptors.add(new CallServerInterceptor(forWebSocket));



`CallServerInterceptor`的`intercept`方法

	@Override 
	public Response intercept(Chain chain) throws IOException {
	    RealInterceptorChain realChain = (RealInterceptorChain) chain;
	    HttpCodec httpCodec = realChain.httpStream();
	    StreamAllocation streamAllocation = realChain.streamAllocation();
	    RealConnection connection = (RealConnection) realChain.connection();
	    Request request = realChain.request();
	
	    long sentRequestMillis = System.currentTimeMillis();
	    httpCodec.writeRequestHeaders(request);
	
	    Response.Builder responseBuilder = null;
	    if (HttpMethod.permitsRequestBody(request.method()) && request.body() != null) {
	      if (responseBuilder == null) {
	        // Write the request body if the "Expect: 100-continue" expectation was met.
	        Sink requestBodyOut = httpCodec.createRequestBody(request, request.body().contentLength());
	        BufferedSink bufferedRequestBody = Okio.buffer(requestBodyOut);
			//数据写入request的body里面
	        request.body().writeTo(bufferedRequestBody);
	        bufferedRequestBody.close();
	      } else if (!connection.isMultiplexed()) {
	        // If the "Expect: 100-continue" expectation wasn't met, prevent the HTTP/1 connection from
	        // being reused. Otherwise we're still obligated to transmit the request body to leave the
	        // connection in a consistent state.
	        streamAllocation.noNewStreams();
	      }
	    }
	
	    httpCodec.finishRequest();
	    if (responseBuilder == null) {
	      responseBuilder = httpCodec.readResponseHeaders(false);
	    }
	
	    Response response = responseBuilder
	        .request(request)
	        .handshake(streamAllocation.connection().handshake())
	        .sentRequestAtMillis(sentRequestMillis)
	        .receivedResponseAtMillis(System.currentTimeMillis())
	        .build();
	    ....
	    return response;
	  }

>RetryAndFollowUpInterceptor->BridgeInterceptor->CacheInterceptor->ConnectInterceptor->CallServerInterceptor
>
这么一长串的调用完后，现再往前查看方法的返回。

`CallServerInterceptor` 已经把获取到的数据写入`request`的`body`里面。然后`request`也放到了`response`，返回给调用者。

 
`ConnectInterceptor`对`response`没做什么处理。直接返回

	  @Override 
		public Response intercept(Chain chain) throws IOException {
		    RealInterceptorChain realChain = (RealInterceptorChain) chain;
		    Request request = realChain.request();
		    StreamAllocation streamAllocation = realChain.streamAllocation();
		
		    // We need the network to satisfy this request. Possibly for validating a conditional GET.
		    boolean doExtensiveHealthChecks = !request.method().equals("GET");
		    HttpCodec httpCodec = streamAllocation.newStream(client, doExtensiveHealthChecks);
		    RealConnection connection = streamAllocation.connection();
		
		    return realChain.proceed(request, streamAllocation, httpCodec, connection);
	  }

`CacheInterceptor`根据是否有头部`Cache-Control`。做了判断。不过对于服务器返回的数据也没做处理

	 @Override
	    public Response intercept(Chain chain) throws IOException {
	        Request request = chain.request();
	
	
	        okhttp3.Response originalResponse = chain.proceed(chain.request());
	        String cacheControl = originalResponse.header("Cache-Control");
	        //String cacheControl = request.cacheControl().toString();
	        LogWraper.d("Novate", maxStaleOnline + "s load cache:" + cacheControl);
	        if (TextUtils.isEmpty(cacheControl) || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
	                cacheControl.contains("must-revalidate") || cacheControl.contains("max-age") || cacheControl.contains("max-stale")) {
	            return originalResponse.newBuilder()
	                    .removeHeader("Pragma")
	                    .removeHeader("Cache-Control")
	                    .header("Cache-Control", "public, max-age=" + maxStale)
	                    .build();
	
	        } else {
	            return originalResponse;
	        }
	    }


`BridgeInterceptor`的`intercept`方法。如果`Content-Encoding`是gzip的话。就做一次处理。一般不会走`if`里面,继续返回

	  @Override 
	public Response intercept(Chain chain) throws IOException {
	    Request userRequest = chain.request();
	    Request.Builder requestBuilder = userRequest.newBuilder();
	
	   ...
		后一个拦截器处理的结果
	    Response networkResponse = chain.proceed(requestBuilder.build());
	
	    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());
	
	    Response.Builder responseBuilder = networkResponse.newBuilder()
	        .request(userRequest);
		
	    if (transparentGzip
	        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
	        && HttpHeaders.hasBody(networkResponse)) {
	      GzipSource responseBody = new GzipSource(networkResponse.body().source());
	      Headers strippedHeaders = networkResponse.headers().newBuilder()
	          .removeAll("Content-Encoding")
	          .removeAll("Content-Length")
	          .build();
	      responseBuilder.headers(strippedHeaders);
	      responseBuilder.body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)));
	    }
	
	    return responseBuilder.build();
	  }

`RetryAndFollowUpInterceptor` 的intercept方法。

	@Override 
	public Response intercept(Chain chain) throws IOException {
	    Request request = chain.request();
	
	    streamAllocation = new StreamAllocation(
	        client.connectionPool(), createAddress(request.url()), callStackTrace);
	
	    int followUpCount = 0;
	    Response priorResponse = null;
	    while (true) {
	      if (canceled) {
	        streamAllocation.release();
	        throw new IOException("Canceled");
	      }
	
	      Response response = null;
	      boolean releaseConnection = true;
	      try {
			//取到后一个拦截器处理的值
	        response = ((RealInterceptorChain) chain).proceed(request, streamAllocation, null, null);
	        releaseConnection = false;
	      } ...
			
			
	      Request followUp = followUpRequest(response);
		//如果没有重定向的。继续返回数据
	      if (followUp == null) {
	        if (!forWebSocket) {
	          streamAllocation.release();
	        }
	        return response;
	      }
	
	     ...
	  }

继续看`AsynCall`的`execute`方法。所有的拦截器走完后，把`respose`返回给调用者。
	
    @Override 
	protected void execute() {
	      boolean signalledCallback = false;
	      try {
			//所有拦截器处理完后的数据
	        Response response = getResponseWithInterceptorChain();
	        if (retryAndFollowUpInterceptor.isCanceled()) {
	          signalledCallback = true;
	          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
	        } else {
	          signalledCallback = true;
				//回调给调用者
	          responseCallback.onResponse(RealCall.this, response);
	        }
	      } catch (IOException e) {
	        if (signalledCallback) {
	          // Do not signal the callback twice!
	          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
	        } else {
	          responseCallback.onFailure(RealCall.this, e);
	        }
	      } finally {
	        client.dispatcher().finished(this);
	      }
	    }
	

这个`responseCallback`也就是我们调用`enqueue`里面传的`Callback`。

	call.enqueue(new Callback() {
			    @Override
			   public void onFailure(Call call, final IOException e) {
			
			    }
			    @Override
			    public void onResponse(Call call, final Response response) throws IOException {
			                
			       }
			  });

小结：
>1  当我们调用enqueue方法时，生成了RealCall,并把我们的回调放入该对象。然后把该对象添加到Dispatcherl类的一个活动队列里面，并且在线程池中执行。   
>2 执行时会run调用execute方法。  
>3 execute会调用我们自定义的拦截器，RetryAndFollowUpInterceptor->BridgeInterceptor->CacheInterceptor->ConnectInterceptor->CallServerInterceptor的intercept方法。  
>4 在ConnectInterceptor的intercept方法建立连接，并且读取数据。然后数据一层一层往回调  
>5 最后在execute方法回调给调用者。此时在子线程。不在UI线程。

拦截器的设计也比较巧妙，通过把所有拦截器放入一个集合，然后控制索引值的改变，依次执行集合里面的拦截器的方法，并且通过继承同样的接口依次将处理的结果往回调