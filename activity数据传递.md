##一个枚举搞定Activity直接的数据传递


正常我们从一个`activity`跳入另一个界面时候，如果需要携带参数。则代码如下


	 public static void startActivity(Activity activity,UserChange userChange){
	        Intent intent = new Intent(activity,UserChangeActivity.class);
	        intent.putExtra(DATA_KEY,userChange);
	        activity.startActivity(intent);
	
	    }


然后跳转的`activity`里面获取数据

	UserChange userChange;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		userChange = (UserChange) getIntent().getSerializableExtra(DATA_KEY);
    }
	

记得以前看`ARouter`源码的时候。要跳转的`activity`只要在字段上面加个注解。就能自动解析传递过来的参数。想着实现起来也不难，于是手动撸起来。


跳转的方法封装先，我们定义了一个通用的key。封装了跳转时要带参数的方法。这样跳转就很简单了，只要把要跳转的class和数据传进去就可以了。



		public final static String INTENT_DATA_KEY="INTENT_DATA_KEY";
		/**
	     * 跳转到Activity
	     *
	     * @param clazz Activity类
	     */
	    protected void startActivity(Class clazz,Object data) {
	        Bundle bundle = new Bundle();
	        putData(bundle,INTENT_DATA_KEY,data);
	        startActivityForResult(clazz, -1, bundle);
	
	    }

		private void putData(Bundle bundle,String key,Object data){
	        if (data instanceof Integer){
	            bundle.putInt(key,(Integer) data);
	        }else if (data instanceof String){
	            bundle.putString(key,(String) data);
	        }else if(data instanceof Serializable){
	            bundle.putSerializable(key,(Serializable) data);
	        }
    	}

接收的地方更简单了，只要在属性上面添加一个枚举修饰

	@IntentData
	UserChange userChange;

当然如果多个参数，则属性的枚举对应的`key`要和这里的`map`里面的`key`一一对应

	/**
     * 跳转到Activity
     *
     * @param clazz Activity类
     */
    protected void startActivity(Class clazz,Map<String,Object> datas) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, Object> entry : datas.entrySet()) {
            putData(bundle,entry.getKey(),entry.getValue());
        }
        startActivityForResult(clazz, -1, bundle);

    }

枚举，很简单.

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IntentData {
	    String key() default INTENT_DATA_KEY;
	
	}

然后就是在`onCreate`里面找到枚举的字段，解析，赋值
	

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveIntentData();
      
    }
	
遍历属性，找到带IntentData枚举的属性
	
    void resolveIntentData(){

     Field[] fields= getClass().getDeclaredFields();
     for (int i=0;i<fields.length;i++){
         Field f = fields[i];
		
         IntentData intentData=f.getAnnotation(IntentData.class);
		//如果属性上面有IntentData修饰
         if (intentData!=null) {
             setIntentData(intentData,f);
         }

     }
    }

一个一个找到后赋值。这里有个陷阱。`Serializable`判断一定要放在最后，因为其他的类型都实现了`Serializeable`接口，如果判断放在最前面，则总是会符合条件。
	
    private void setIntentData(IntentData intentData,Field f){
        if (intentData!=null){
            Intent intent = getIntent();
            //获取注解字段的类型
            Class clazz=f.getType();
            Object o = null;
            String key = intentData.key();
			//根据字段的类型获取
            if (Integer.class==clazz||int.class==clazz){
                o = intent.getIntExtra(key,0);
            }else if (String.class==clazz){
                o = intent.getStringExtra(key);
            } else if (Boolean.class==clazz||boolean.class==clazz){
                o = intent.getBooleanExtra(key,false);
            }else if (Serializable.class.isAssignableFrom(clazz)){
                o = intent.getSerializableExtra(key);
            }
			//如果获取到，赋值
            if (o!=null) {
                ReflectUtil.setField(this, f.getName(), o);
            }
        }
    }


赋值的操作就比较简单。当然也可以像`ButterKnife`那样。在编译期自动生成代码处理。只不过那样难度大些。

 	public static Field getField(Class<?> clazz, String fieldName) {
        Field field = null;

        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException var4) {
            var4.printStackTrace();
        }

        return field;
    }

    public static void setField(Object obj, String fieldName, Object value) {
        Field field = getField(obj.getClass(), fieldName);
        if (field != null) {
            try {
                field.set(obj, value);
            } catch (IllegalAccessException var5) {
                var5.printStackTrace();
            }
        }

    }

把上述代码封装在`BaseActivity`里面，则后面跳转界面传递参数就直接调用下面封装方法。

		/**
	     * 跳转到Activity
	     *
	     * @param clazz Activity类
	     */
	    protected void startActivity(Class clazz,Object data) {
	        Bundle bundle = new Bundle();
	        putData(bundle,INTENT_DATA_KEY,data);
	        startActivityForResult(clazz, -1, bundle);
	
	    }



