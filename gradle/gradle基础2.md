###buildTypes

|属性名|含义|
|-|-|
|applicationIdSuffix|配置基于默认applicationId的后缀
|debuggable|是否生成一个可供调试的Apk
|jniDebuggable|是否生成一个可供调试JNI代码的Apk
|minifyEnabled|是否启用Proguard混淆
|multiDexEnabled|是否启用自动拆分多个Dex的功能
|zipAlignEnabled|是否开启开启zipalign优化，提高apk运行效率
|shrinkResources|是否自动清理未使用的资源，默认为false
|proguardFile|配置Proguard混淆使用的配置文件
|proguardFiles|同时配置多个ProGuard配置文件
|signingConfig|配置默认的签名信息，也是一个ProductFlavor，可直接配置


	android{
	    
	    buildTypes{
	        release{
	            minifyEnabled false
	            proguardFiles getDefaultPraguardFile('proguard-andrcid.txt'), 'proguard-rules.pro'
	        }
	    }



##signingConfigs
|属性名 	|含义
|-|-|
|storeFile |	签名证书文件
|storePassword |	签名证书文件的密码
|storeType 	|签名证书的类型
|keyAlias 	|签名证书中密钥别名
|keyPassword 	|签名证书中该密钥的密码

	android {
	    signingConfigs {
	        release{
	            storeFile file('myFile.keystore')
	            storePassword 'psw'
	            keyAlias 'myKey'
	            keyPassword 'psw'
	        }
	    }
	}


##productFlavors

作用：添加不同的渠道、并对其做不同的处理

|属性名|含义
|-|
|applicationId|设置该渠道的包名
|consumerProguardFiles|对aar包进行混淆
|manifestPlaceholders|设置该渠道的manifest文件
|multiDexEnabled|启用多个dex的配置，可突破65535方法问题
|proguardFiles|混淆使用的文件配置
|signingConfig|签名配置
|testApplicationId|适配测试包的包名
|testFunctionalTest|是否是功能测试
|testHandleProfiling|是否启用分析功能
|testInstrumentationRunner|配置运行测试使用的Instrumentation Runner的类名
|testInstrumentationRunnerArguments|配置Instrumentation Runner使用的参数
|useJack|标记是否启用Jack和Jill这个全新的、高性能的编译器
|dimension|维度，通过flavorDimensions方法声明，声明前后代表优先级

//定义baidu和google两个渠道，并声明两个维度，优先级为abi>version>defaultConfig

	android{
	    flavorDimensions "abi", "version"
	    productFlavors{
	        google{
	            dimension "abi"
	        }
	       baidu{ 
	           dimension "version"
	       } 
	}

###buildConfigFiled

作用：在buildTypes、ProductFlavor自定义字段等配置  
方法：buildConfigField(String type,String name,String value)

	android{
	   buildTypes{
	        debug{
	            buildConfigField "boolean", "LOG_DEBUG", "true"
	            buildConfigField "String", "URL", ' "http://www.ecjtu.jx.cn/" '
	        }
	    }
	}

###多渠道构建

####基本原理
构建变体（Build Variant）=构建类型（Build Type）+构建渠道（Product Flavor）

assemble开头的负责生成构件产物(Apk)

构建方式：通过占位符manifestPlaceholders实现

	//AndroidManifest
	<meta-data 
	    android: value="Channel ID" 
	    android:name="UMENG_ CHANNEL"/>
	//build.gradle
	android{
	    productFlavors{
	        google{
	            manifestPlaceholders.put("UMENG_ CHANNEL", "google")
	        }
	       baidu{
	            manifestPlaceholders.put("UMENG_ CHANEL", "baidu")
	       }
	}

	//改进：通过productFlavors批量修改
	android{
	    productFlavors{
	        google{
	        }
	       baidu{
	       }
	       ProductFlavors.all{ flavor->
	           manifestPlaceholders.put("UMENG_ CHANEL", name) 
	       }        
	}


###使用共享库

	//声明需要使用maps共享库，true表示如果手机系统不满足将不能安装该应用
	<uses-library
	    android:name="com.google.android.maps"
	    android:required="true" 
	/>

###adb选项配置

|属性名|含义
|-|
|timeOutInMs|设置执行adb命令的超时时间，单位毫秒  
|installOptions|设置adb install安装设置项
-l：锁定该应用程序  
-r：替换已存在的应用程序，即强制安装  
-t：允许测试包  
-s：把应用程序安装到SD卡上  
-d：允许进行降级安装，即安装版本比手机自带的低  
-g：为该应用授予所有运行时的权限  

	android{
	    adbOptions{
	        timeOutInMs = 5*1000
	        installOptions '-r', '-s'
	    }
	}


###DEX选项配置

|属性名|含义
|-|
|incremental：|配置是否启用dx的增量模式，默认值为false
|javaMaxHeapSize：|配置执行dx命令时为其分配的最大堆内存
|jumboMode：|配置是否开启jumbo模式
|preDexLibraries：|配置是否预dex Libraries库工程，默认值为true，开启后会提高增量构建的速度
|threadCount：|配置Android Gradle运行dx命令时使用的线程数量

	android{
	 dexOptions {
	        incremental true
	        javaMaxHeapSize "4g" //specify the heap size for the dex process
	        preDexLibraries = true //delete the already predexed libraries
	        maxProcessCount 8
	    }
	}

