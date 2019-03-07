##dragger2初体验


添加依赖

 	compile 'com.google.dagger:dagger:2.11'
    annotationProcessor 'com.google.dagger:dagger-compiler:2.11'


在BaseActivity里面用Injecet注解，标明需要注入的对象，那如果关联需要注入的对象呢?

		public abstract class BaseActivity<P extends BasePresenter> extends AppCompatActivity implements IBaseView {
	
	
	    @Inject
	    protected P mPersenter;
	 
	    @Override
	    protected void onCreate(@Nullable Bundle savedInstanceState) {
	        requestWindowFeature(Window.FEATURE_NO_TITLE);
	        super.onCreate(savedInstanceState);
	        setContentView(getLayoutResID());
	        initData();
	    }
	    /**
	     * 初始化数据
	     */
	    public abstract void initData();
	
	    /**
	     * 布局资源文件
	     * @return 布局资源文件
	     */
	    public abstract int getLayoutResID();
	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        if (mPersenter!=null) {
	            mPersenter.detachView();
	        }
	    }
	}

下面添加一个类，并且用Module注解，里面有个方法，返回一个DraggerTestPresenter类型，这个类型即是我们即将注入到Activity里面的P。同样的该方法前面添加一个Provides注解。

	@Module
	public class MainModule {
	    @Provides
	    DraggerTestPresenter providerP(){
	        return new DraggerTestPresenter();
	    }
	}

再添加接口，并且用Component注解。这个注解里面的modules值可以是多个。再添加一个方法，里面的参数就是需要注入的类

	@Component(modules = {MainModule.class})
	public interface MainComponent {
	    void inject(DraggerTestActivity draggerTestActivity);
	}

P是一个很简单的。

	public class DraggerTestPresenter extends BasePresenter {
	    public String hello(){
	        return "hello Dragger";
	    }
	}
然后再Activity里面调用mPresenter方法。

	public class DraggerTestActivity extends BaseActivity<DraggerTestPresenter> {
	
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_aop);
	
	        setTitle("DraggerTestActivity");
	       LogUtil.d(mPersenter.hello());
	
	    }
	
	    @Override
	    public void initData() {
	
	    }
	
	    @Override
	    public int getLayoutResID() {
	        return R.layout.activity_aop;
	    }
	}

很不幸，奔溃了。。

![](https://i.imgur.com/8ZPhym3.png)

很明显，mPersenter还没注入进来。因为在activity。我们还没有调用任何让他们关联的代码，在activity里面添加`DaggerMainComponent.create().inject(this);`代码。
	public class DraggerTestActivity extends BaseActivity<DraggerTestPresenter> {
	
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_aop);
	        setTitle("DraggerTestActivity");
	        DaggerMainComponent.create().inject(this);
	       LogUtil.d(mPersenter.hello());
	
	    }
	
	    @Override
	    public void initData() {
	
	    }
	
	    @Override
	    public int getLayoutResID() {
	        return R.layout.activity_aop;
	    }
	}

DaggerMainComponent类又是哪来的？
![](https://i.imgur.com/iQzBxEj.png)

如图。当我们编译的时候。dragger编译器已经帮我们生存了一个DraggerMainComponent类。并且该类实现了我们的MainComponent接口。和butterknife很相似。当我们调用`DaggerMainComponent.create().inject(this);`时，就像我们调用`ButterKnife.bind(this);`一样注入对象。这样我们可以不用创建，就可以使用该对象了。  
