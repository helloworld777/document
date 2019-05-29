ARouter源码解读

以前看优秀的开源项目，看到了页面路由框架ARouter，心想页面路由是个啥东东，于是乎网上搜索查看，是阿里出品开源的，主要是关于页面跳转的解耦框架。一直想看看具体是怎么实现的，今有时间便来一探究竟。

传统的页面跳转就是调用系统的startActivity，里面的参数Intent携带了要跳转的信息，可以传入要跳转的activity信息或者action。如果是action则要在清单文件里面配置。但是ARouter的实现页面跳转则另辟蹊径,传入一个path路径，官方示例代码如下

    ARouter.getInstance().build("/test/activity2") .navigation();

初看到这样的代码你会想，这是要跳转到哪里去呢?既然只有一个路径，那么某个activity必然会和这个路径有关系.果然在示例代码的Test2Activity上面有个注解,里面就有个这个参数。

![image](https://upload-images.jianshu.io/upload_images/6029641-c13f8e6aed4a1d4f.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

基于以前注解的知识，作猜想,只要获取Activity上面的注解参数，再把该参数和该Activity绑定起来，全局缓存，只要匹配跳转路径是该Activity上注解参数，就让它跳转到该Activity.大概应该是这样。有了这个猜想，再去查看源码，看看是否符合猜想。下面开始探索源码之旅。

首先调用其初始化方法

     ARouter.init(getApplication());

点到这个ARouter类init方法里面，发现里面调用的是一个_ARouter的初始化方法，再初看其他方法，发现所有方法其实都是调用的_ARouter类的方法。

看_ARouter类的初始化方法

     protected static synchronized boolean init(Application application) {    

         mContext = application;   
        LogisticsCenter.ini(mContext, executor);//LogisticsCenter类的初始化    
        logger.info(Consts.TAG, "ARouter init success!");//日志初始化   
		hasInit= true;//根据该变量判断是否初始化了，若使用前未调用初始化方法，则抛出未初始化异常信息    
        if (Build.VERSION.SDK_INT >     Build.VERSION_CODES.ICE_CREAM_SANDWICH) {    //                 
             application.registerActivityLifecycleCallbacks(new           
             AutowiredLifecycleCallback());    
   		 }   

		return true;

    }

其主要是LogisticsCenter类的初始化。

LogisticsCenter类的初始化主要都做了什么事情呢？

继续往下看

	 public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws HandlerException {   
		...前面省略无关代码
	
		 Set routerMap;        // It will rebuild router map every times when debuggable.        
	
	 	if (ARouter.debuggable() || PackageUtils.isNewVersion(context){            
		
			//获取Arouter自动生成的类的信息           
		  routerMap = ClassUtils.getFileNameByPackageNam(mContext, ROUTE_ROOT_PAKCAGE);           
		 
		  if (!routerMap.isEmpty()) {                
				context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).edit().putStringSet(AROUTER_SP_KEY_MAP*, routerMap).apply();            }            
		 
				 PackageUtils.updateVersion(context);    // Save new version name when router map update finish.       
	 
	  	} else {                    
	
	 		routerMap = new HashSet<>(context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).getStringSet(*AROUTER_SP_KEY_MAP*, new HashSet()));        
		}        
	 
	     
	 startInit = System.currentTimeMillis*();

	 //将路径按分类加入缓存       
	 
	  for (String className : routerMap) {           
	 
	  	if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {               
	 
	 		// This one of root elements, load root.                
			((IRouteRoot) (Class.*forName*(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex*);           
		 } else if (className.startsWith(*ROUTE_ROOT_PAKCAGE *+ *DOT *+ *SDK_NAME *+ *SEPARATOR *+ *SUFFIX_INTERCEPTORS*)) {               
	 
	  // Load interceptorMeta                
				((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);           
		} else if (className.startsWith(*ROUTE_ROOT_PAKCAGE *+ *DOT *+ *SDK_NAME *+ *SEPARATOR *+ *SUFFIX_PROVIDERS*)) {                
	 
	 // Load providerIndex                
	 
	 ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.*providersIndex*);           
			 }       
		 }
		...后面省略无关代码     
	 }
	
	 执行完*getFileNameByPackageName**这个方法后,Set里面都有些什么呢？*

![image](https://upload-images.jianshu.io/upload_images/6029641-8066894f910e6f83.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

*缓存类*

![image](https://upload-images.jianshu.io/upload_images/6029641-f174f4d4b6a15151.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

初始化完毕。

小结:初始化方法只是要把缓存里面的groupsIndex和providerIndex和intercaptorsIndex三个Map集合先缓存了。groupsIndex主要是组名的路径和对象class映射。什么是组名的路径呢？

比如当前示例传入的路径为/test/activity2,则组名就是test。providerIndex主要是用于依赖注入的path路径和class对应的关系。至于啥事依赖注入，就是声明一个对象,并不实例化，由框架根据你传入的参数生成对应的对象,说白点就是通过框架实例化你所需要的对象(试想连对象都不用自己实例化了，是不是耦合性就非常低了)。如果知道java web spring 框架的话，那么这个就很清楚了，因为spring框架里面就大量应用了依赖注入。InterceptorsIndex则是保存了拦截器的path和class对应的关系，何为拦截器，拦截什么操作？，继续看下面代码。

现在查看调用代码，

	 ARouter.*getInstance*().build("/test/activity2").navigation();

Build方法调用_ARouter方法的build方法

	 public Postcard build(String path) {    
		return _ARouter.*getInstance*().build(path);
	}

查看_ARouter方法


 	protected Postcard build(String path) {    

		if (TextUtils.isEmpty(path)) {        
			throw new HandlerException(Consts.TAG + "Parameter is invalid!");   
		} else {        
 
 			PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);        
 			if (null != pService) {           
				 path = pService.forString(path);       
		 }        
 
 		return build(path, extractGroup(path));    
	}}
 
 其中extractGroup方法是根据路径解析其路径所在的组。
 
 
 
	 private String extractGroup(String path) {    
	 
		 if (TextUtils.*isEmpty*(path) || !path.startsWith("/")) {        
			throw new HandlerException(Consts.*TAG *+ "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");    
		 
		 }    
		try {
		 
		 //如果当前路径是/xxx/... 则默认其在xxx组内.         
		
		 String defaultGroup = path.substring(1, path.indexOf("/", 1));       
		
		  if (TextUtils.*isEmpty*(defaultGroup)) {            
		 
			 throw new HandlerException(Consts.*TAG *+ "Extract the default group failed! There's nothing between 2 '/'!");        
		 
		 } else {            
			return defaultGroup;        
		}    
		 
		 } catch (Exception e) {       
		 
		  *logger*.warning(Consts.*TAG*, "Failed to extract default group! " + e.getMessage());        return null;    
	}}

最终调用这个方法

 */**** * Build postcard by path and group** */*

	 protected Postcard build(String path, String group) {   
	
		 if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {       
		
			 throw new HandlerException(Consts.TAG + "Parameter is invalid!");   
		 
		 } else {        
			PathReplaceService pService = ARouter.getInstance().navigation(PathReplaceService.class);      
		  	if (null != pService) {            
				path = pService.forString(path);       
			 }       
		
		 return new Postcard(path, group);    
	}}

这个方法后返回一个新的Postcard对象。

先查看Postcard对象的navigation方法,里面又多个重载方法，但最后都调用了此方法。

	 public Object navigation(Context mContext, Postcard postcard, int requestCode, NavigationCallback callback) {    
	 	return _ARouter.getInstance().navigation(mContext, postcard, requestCode, callback);
	}

还是调用_ARouter的navigation方法

查看navigation方法



 	protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {   
		 try {        
			LogisticsCenter.completion(postcard);   
		} catch (NoRouteFoundException ex) {      
			  *logger*.warning(Consts.*TAG*, ex.getMessage());
	..//后面代码暂时省略



	public synchronized static void completion(Postcard postcard) {        

		//从缓存中获取path对应的RouteMeta类型    
		//RouteMeta中保存了该路径对应的目标的Class类型    
		RouteMeta routeMeta = Warehouse.*routes*.get(postcard.getPath());   

		//如果没有,去加载    
		if (null == routeMeta) {    

			// Maybe its does't exist, or didn't load.        
			//查找所在组群的类信息       

			Class groupMeta = Warehouse.groupsIndex.get(postcard.getGroup());  

			// Load route meta.        

			if (null == groupMeta) {            
				 throw new NoRouteFoundException(*TAG *+ "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");       
	  		} else {            
				// Load route and cache it into memory, then delete from metas.               

				IRouteGroup iGroupInstance = groupMeta.getConstructor().newInstance();

				//加载该群组下面的路径和class对应的信息                

				iGroupInstance.loadInto(Warehouse.routes);                
 
				Warehouse.groupsIndex.remove(postcard.getGroup());                    
				completion(postcard);   
				// Reload        
			}    
		}else {        

			postcard.setDestination(routeMeta.getDestination());    

			//设置跳转的class信息       
 
			postcard.setType(routeMeta.getType());              
			//设置路由类型        
			postcard.setPriority(routeMeta.getPriority());      
			//路由优先级       
			postcard.setExtra(routeMeta.getExtra());        
			Uri rawUri = postcard.getUri();       
			if (null != rawUri) {   
				// Try to set params into bundle.            

				Map resultMap = TextUtils.*splitQueryParameters*(rawUri);            

				Map paramsType = routeMeta.getParamsType();           
				 if (MapUtils.isNotEmpty(paramsType)) {               
					 // Set value by its type, just for params which annotation by @Param                
					for (Map.Entry params : paramsType.entrySet()) {                    
						setValue(postcard,  params.getValue(), params.getKey(),resultMap.get(params.getKey()));
				}                
				// Save params name which need auto inject.               

				postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));            
			}            // Save raw uri            
		
				postcard.withString(ARouter.*RAW_URI*, rawUri.toString());       
		 }...//如果是acitivty跳转，下面代码省略暂时不看      
	 }

该方法主要是设置postcard对象里面的要跳转的信息，和跳转时如果有参数则设置参数。

那么如何使将path和对应的class类信息加载进来的呢？

通过调试可以发现是调用了ARouter$$Group$$test的loadInto方法

![image](https://upload-images.jianshu.io/upload_images/6029641-fd5601197eb3395a.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

找到该类的该方法,loadInto方法里面有设置路径和对应的RoteMeta对象一一对应，而RoteMeta对象则是保存了相应的属性,比如类型,和跳转的目标类类型。该类没有在src目录下，而是在build目标下，由此可见该类是自动编译的。

![image](https://upload-images.jianshu.io/upload_images/6029641-ca4f84066d0e0688.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

但是是如何自动编译的呢，这个问题暂时放下。

继续往下看，既然已经找到了path和要跳转的activity class的信息了，那么接下应该就是调用真正的跳转方法了。继续navigation方法往下看.



	protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {

 		//省略前面代码...

		//如果设置了greenChannel为true，表示不需要拦截，为false。则跳转前还需做一步拦截操作        
		if (!postcard.isGreenChannel()) {  
 		// It must be run in async thread, maybe interceptor cost too mush time made ANR.
		//前面初始化分析的拦截器作用就在此       

			interceptorService.doInterceptions(postcard, new InterceptorCallback() {           
				 @Override           
				
				 public void onContinue(Postcard postcard) {              
				
				 _navigation(context, postcard, requestCode, callback);            }                  });  
				
				 } else {       
				
				return _navigation(context, postcard, requestCode, callback);  
				
				 }    
		return null;
	}

最后调用了_navigation方法。

	private Object _navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {   
		final Context currentContext = null == context ? mContext: context;    
		switch (postcard.getType()) {        
			case ACTIVITY:            // Build intent            
 			final Intent intent = new Intent(currentContext, postcard.getDestination());            
			intent.putExtras(postcard.getExtras());//设置要跳转携带的参数            
		// Set flags.           
			int flags = postcard.getFlags();            

 			if (-1 != flags) {                
				intent.setFlags(flags);           
			 }else if (!(currentContext instanceof Activity)) {    
				// Non activity, need less one flag.               
				intent.setFlags(Intent.*FLAG_ACTIVITY_NEW_TASK*);           
			 }            
			// Navigation in main looper.            

		new Handler(Looper.getMainLooper()).post(new Runnable() {                

		@Override                
		public void run() {                   

			if (requestCode > 0) {  // Need start for result                        
				ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());                   

			} else {

			//真正的跳转动作                        

			ActivityCompat.*startActivity*(currentContext, intent, postcard.getOptionsBundle());                   
			 }                              
		 });            

		break;       
		//如果类型是acitivity则，后面的可以省略不看   
	 return null;
	}

终于看到了startActivity方法了。

查看跳转到Test1Activity的示例代码时，里面携带了要传入Test1Activity的参数，但是Test1Activity里面没有看到获取参数代码，而是每个参数上面还有@Autowited注解

 	@Route(path = "/test/activity1")
	public class Test1Activity extends AppCompatActivity {    
		@Autowired    String name = "jack";

还有onCreate方法里面的调用了inject方法.由此可猜测该方法应该已经把@Autowited下面的变量全都赋值了。

	ARouter.getInstance().inject(this);

追踪查看该方法最终调用了_ARouter里面的inject方法

	static void inject(Object thiz) {    
		AutowiredService autowiredService = ((AutowiredService) ARouter.*getInstance*().build("/arouter/service/autowired").navigation());    
		if (null != autowiredService) {        
			autowiredService.autowire(thiz);    
		}
	}

AutowiredService 是个接口，其实现是AutowiredServiceImpl类

查看AutowiredServiceImpl类的autowite方法

	@Override
	public void autowire(Object instance) {    
 
		String className = instance.getClass().getName();    

		 try {        
			if (!blackList.contains(className)) {
				ISyringe autowiredHelper = classCache.get(className);            
 
				if (null == autowiredHelper) {  
				// No cache.
				//查找对应的ISyringe 实现类,className$$ARouter$$Autowired
				autowiredHelper = (ISyringe) Class.forName(instance.getClass().getName() + SUFFIX_AUTOWIRED*).getConstructor().newInstance();            
			}            

			autowiredHelper.inject(instance);            

			classCache.put(className, autowiredHelper);       
		 }   
	 	}catch (Exception ex) {        
		blackList.add(className);    // This instance need not autowired.    
		}
	}

最终调用了className$$ARouter$$Autowired类里面的inject

方法，看到这个类的类名字，在该工程查找该类，发现也在build目录下

![image](https://upload-images.jianshu.io/upload_images/6029641-c723b4b6fc03feee.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

可以看到最终这里调用getIntent().getXXX获取Intent携带的参数。因为几乎所有获取携带参数的代码方式是一样的，因此这里直接也是用自动编译而成的。

小结:梳理下跳转界面的步骤 1 首先根据路径生成一个路径和组名的Postcard对象。2查看缓存里面是否有组名的class信息，如果没有，根据组名获取所有组名下面的的class信息。3 根据获取的信息填充postcard对象。4 如果需要拦截，如执行拦截器的方法。5 如果没有拦截，执行跳转动作. 6 跳转的那个activity执行注入参数方法。

总结:ARouter的界面跳转流程分析完毕。但是ARouter的用处不止界面跳转，还有依赖注入等其他功能,根据github官网介绍

1. **支持直接解析标准URL进行跳转，并自动注入参数到目标页面中**

2. **支持多模块工程使用**

3. **支持添加多个拦截器，自定义拦截顺序**

4. **支持依赖注入，可单独作为依赖注入框架使用**

5. **支持InstantRun**

6.**支持MultiDex**(Google方案)

7.映射关系按组分类、多级管理，按需初始化

8\. 支持用户指定全局降级与局部降级策略

9\. 页面、拦截器、服务等组件均自动注册到框架

10\. 支持多种方式配置转场动画

11\. 支持获取Fragment

12\. 完全支持Kotlin以及混编(配置见文末 其他#5)

有兴趣可以自行查看其他功能详解

[ARouter github地址](https://github.com/alibaba/ARouter)
