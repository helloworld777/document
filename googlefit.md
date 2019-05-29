##Googlefit接入指南

[官方文档使劲戳这里](https://developers.google.com/fit/android/)  
如果无法加载，少年你需要翻墙再点。

因为app里面接入了googlefit。并且一直有客户投诉说googlefit上传数据不准确。查看googlefit开发官网才发现，googfit api都已经更新了。并且接入过程中也遇到一些坑。在此记录以防后面再入坑


build.gradle添加依赖 

	dependencies {
		...
	  compile 'com.google.android.gms:play-services-fitness:16.0.1'
      compile 'com.google.android.gms:play-services-auth:16.0.1'
		...
	}

构建FitnessOptions，DataType表示你要写入到googlfit的数据类型，数据类型有很多，根据需要添加

	FitnessOptions fitnessOptions = FitnessOptions.builder()
					//步数
	                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
					//距离
	                .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
	                .build();

//下面登录google账号。并且授权上面添加的那些数据授权的弹框

	public void connectGoogle() {
	        try {
				//如果已经登录过google账号。则可以拿到账号
	            account=GoogleSignIn.getLastSignedInAccount(activity);
	            if (account==null) {
					//没有。则要登录
	                signIn();
	            }else{
					//有，则要订阅
	                sunbscrerib();
	            }
	        }catch (Exception e){
	            e.printStackTrace();
	           
	        }
	    }

 	

//此方法会弹框google账号登录的弹框

	private void signIn(){
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }
	
	/**
     * 在activity的onActivityResult方法里面调用，回调是否登录账号成功。
     * @param data
     */
    public void handleSignInResult(int requestCode,Intent data) {
        if (requestCode!=GOOGLE_SIGN_IN){
            return;
        }
        try {
            GoogleSignInResult result=getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                account = getSignedInAccountFromIntent(data).getResult(ApiException.class);
                sunbscrerib();
            }
        } catch (ApiException e) {
            dAndSave("signInResult:failed code=" + e.getStatusCode());
        }
    }

	private void sunbscrerib(){
	            account=GoogleSignIn.getLastSignedInAccount(activity);
	            if (account==null){
			        return;
	            }
			//判断是否有写入数据的权限，这个会弹出授权写入数据的弹框
	        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
	            GoogleSignIn.requestPermissions(
	                    activity, // your activity
	                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
	                    account,
	                    fitnessOptions);
	        }else{
				  subscriptionData();
			}
	    }
	
如果已经有权限了，则订阅，订阅成功后就可以上传数据了

	 private void subscriptionData() {
        subscribe(DataType.TYPE_STEP_COUNT_DELTA);// 订阅步数
        subscribe(DataType.TYPE_DISTANCE_DELTA);// 距离
    }

	// 订阅
	    private void subscribe(final DataType dataType) {
	            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
	            if (account == null) {
	                return;
	            }
	        Fitness.getRecordingClient(activity, GoogleSignIn.getLastSignedInAccount(activity))
	                .subscribe(dataType)
	                .addOnSuccessListener(new OnSuccessListener<Void>() {
	                    @Override
	                    public void onSuccess(Void aVoid) {
	                        //每订阅成功一个，会回调这个方法
	                    }
	                })
	                .addOnFailureListener(new OnFailureListener() {
	                    @Override
	                    public void onFailure(@NonNull Exception e) {
	                    
	                    }
	                });
	    }




	DataSet stepDataSet;
    DataSet distanceDataSet;
	//创建要上传的步数数据对象
	stepDataSet = createDataForRequest(DataType.TYPE_STEP_COUNT_DELTA,Field.FIELD_STEPS, stepAllCount, lastUploadTime, currentUploadTime);// 步数
	//创建要上传的距离数据对象
    distanceDataSet = createDataForRequest(DataType.TYPE_DISTANCE_DELTA,Field.FIELD_DISTANCE ,allDistance, lastUploadTime, currentUploadTime);// 距离

	
	 /**
     * //创建要上传的数据对象
     * @param dataType DataType类型
     * @param field Field类型
     * @param values 数据值
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return
     * 开始时间戳必须小于结束时间戳，不然就抛异常
     */
	private DataSet createDataForRequest(DataType dataType,Field field, Object values, long startTime, long endTime) {
        DataSource dataSource =
                new DataSource.Builder()
                        .setAppPackageName(activity)
                        .setDataType(dataType)
                        .setStreamName("streamName")
                        .setType(DataSource.TYPE_RAW)
                        .build();
        DataSet dataSet = DataSet.create(dataSource);
        DataPoint dataPoint =
                dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        if (dataType == DataType.TYPE_CALORIES_EXPENDED||dataType==DataType.TYPE_HEART_RATE_BPM) {
            dataPoint.getValue(field).setFloat((Float) values);
        } else {
			//如果是float类型则要调用setFloagValues	
            if (values instanceof Integer) {
                dataPoint.setIntValues((Integer) values);
            } else {

                dataPoint = dataPoint.setFloatValues((Float) values);
            }
        }
        dataSet.add(dataPoint);
        return dataSet;
    }

数据已经准备好了。现在上传到googlefit。因为是网络操作，因此在后台线程调用

 	new AsyncTaskUtil(new AsyncTaskUtil.IAsyncTaskCallBack() {
            @Override
            public Object doInBackground(String... arg0) {
                Task<Void> responseStep = Fitness.getHistoryClient(activity, GoogleSignIn.getLastSignedInAccount(activity)).insertData(stepDataSet);
                Task<Void> responseDistance = Fitness.getHistoryClient(activity, GoogleSignIn.getLastSignedInAccount(activity)).insertData(distanceDataSet);
              

                //等待任务完成
                while(!responseStep.isComplete()||!responseDistance.isComplete()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
               
                return null;
            }

            @Override
            public void onPostExecute(Object result) {
				//数据上传完成
            }
        }).execute("");

如果数据上传成功。打开googlefit app就会看到googlefit 里面的数据已经更新了。