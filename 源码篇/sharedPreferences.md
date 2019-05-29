##SharePreference源码

`ContextImpl` `getSharedPreferences`方法， `mSharedPrefsPaths` 是个集合`ArrayMap<String, File>`，根据名称缓存了文件


		 public SharedPreferences getSharedPreferences(String name, int mode) {
	        // At least one application in the world actually passes in a null
	        // name.  This happened to work because when we generated the file name
	        // we would stringify it to "null.xml".  Nice.
	        if (mPackageInfo.getApplicationInfo().targetSdkVersion <
	                Build.VERSION_CODES.KITKAT) {
	            if (name == null) {
	                name = "null";
	            }
	        }
	
	        File file;
	        synchronized (ContextImpl.class) {
	            if (mSharedPrefsPaths == null) {
	                mSharedPrefsPaths = new ArrayMap<>();
	            }
	            file = mSharedPrefsPaths.get(name);
	            if (file == null) {
					// 1如果不存在，则创建
	                file = getSharedPreferencesPath(name);
	                mSharedPrefsPaths.put(name, file);
	            }
	        }
			//这个时候已经有文件了
	        return getSharedPreferences(file, mode);
	    }
	//注释 1  /baoming/data/shared_prefs目录下
	public File getSharedPreferencesPath(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }
	
	private File getPreferencesDir() {
        synchronized (mSync) {
            if (mPreferencesDir == null) {
                mPreferencesDir = new File(getDataDir(), "shared_prefs");
            }
            return ensurePrivateDirExists(mPreferencesDir);
        }
    }

	  @Override
    public SharedPreferences getSharedPreferences(File file, int mode) {
        SharedPreferencesImpl sp;
        synchronized (ContextImpl.class) {
			//1 从缓存中获取SharedPreferencesImpl对象。根据进程名获取当前进程的
            final ArrayMap<File, SharedPreferencesImpl> cache = getSharedPreferencesCacheLocked();
            sp = cache.get(file);
            if (sp == null) {
				//检查mode
                checkMode(mode);
                if (getApplicationInfo().targetSdkVersion >= android.os.Build.VERSION_CODES.O) {
                    if (isCredentialProtectedStorage()
                            && !getSystemService(UserManager.class)
                                    .isUserUnlockingOrUnlocked(UserHandle.myUserId())) {
                        throw new IllegalStateException("SharedPreferences in credential encrypted "
                                + "storage are not available until after user is unlocked");
                    }
                }
				//2 生成一个SharedPreferencesImpl，放入缓存
                sp = new SharedPreferencesImpl(file, mode);
                cache.put(file, sp);
                return sp;
            }
        }
        if ((mode & Context.MODE_MULTI_PROCESS) != 0 ||
            getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.HONEYCOMB) {
            // If somebody else (some other process) changed the prefs
            // file behind our back, we reload it.  This has been the
            // historical (if undocumented) behavior.
            sp.startReloadIfChangedUnexpectedly();
        }
        return sp;
    }
	
	//注释1 。根据包名，也就是进程名获取缓存。
	 private ArrayMap<File, SharedPreferencesImpl> getSharedPreferencesCacheLocked() {
	        if (sSharedPrefsCache == null) {
	            sSharedPrefsCache = new ArrayMap<>();
	        }
	
	        final String packageName = getPackageName();
	        ArrayMap<File, SharedPreferencesImpl> packagePrefs = sSharedPrefsCache.get(packageName);
	        if (packagePrefs == null) {
	            packagePrefs = new ArrayMap<>();
	            sSharedPrefsCache.put(packageName, packagePrefs);
	        }
	
	        return packagePrefs;
	    }

	//注释2 生成一个实现类
	 SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false;
        mMap = null;
        startLoadFromDisk();
    }
	//从磁盘上加载
    private void startLoadFromDisk() {
        synchronized (mLock) {
            mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            public void run() {
                loadFromDisk();
            }
        }.start();
    }
	
	private void loadFromDisk() {
        synchronized (mLock) {
            if (mLoaded) {
                return;
            }
			//如果备份文件存在
            if (mBackupFile.exists()) {
                mFile.delete();
                mBackupFile.renameTo(mFile);
            }
        }

        // Debugging
        if (mFile.exists() && !mFile.canRead()) {
            Log.w(TAG, "Attempt to read preferences file " + mFile + " without permission");
        }

        Map map = null;
        StructStat stat = null;
        try {
            stat = Os.stat(mFile.getPath());
            if (mFile.canRead()) {
                BufferedInputStream str = null;
                try {
                    str = new BufferedInputStream(
                            new FileInputStream(mFile), 16*1024);
					//xml文件格式。用Pull方式去解析
                    map = XmlUtils.readMapXml(str);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot read " + mFile.getAbsolutePath(), e);
                } finally {
                    IoUtils.closeQuietly(str);
                }
            }
        } catch (ErrnoException e) {
            /* ignore */
        }

        synchronized (mLock) {
            mLoaded = true;
            if (map != null) {
                mMap = map;
                mStatTimestamp = stat.st_mtim;
                mStatSize = stat.st_size;
            } else {
                mMap = new HashMap<>();
            }
            mLock.notifyAll();
        }
    }




	 public Editor edit() {
	        // TODO: remove the need to call awaitLoadedLocked() when
	        // requesting an editor.  will require some work on the
	        // Editor, but then we should be able to do:
	        //
	        //      context.getSharedPreferences(..).edit().putString(..).apply()
	        //
	        // ... all without blocking.
	        synchronized (mLock) {
				//如果前面的xml文件没有加载完成。等待。。
	            awaitLoadedLocked();
	        }
			//返回一个新的EditorImpl对象
	        return new EditorImpl();
	    }

小结:`SharedPreferencesImpl`初始化的时候会创建一个线程把文件加载进来。如果文件还没加载进来调用`edit`方法则会阻塞当前调用线程。

	//put的数据都放在mModified map集合中。
 	public Editor putString(String key, @Nullable String value) {
            synchronized (mLock) {
                mModified.put(key, value);
                return this;
            }
    }

	public boolean commit() {
            long startTime = 0;

            if (DEBUG) {
                startTime = System.currentTimeMillis();
            }
			//注释1 把mModefied的里面的值放到mMap里面
            MemoryCommitResult mcr = commitToMemory();
			
			//注释2 放入QueuedWork队列。QueuedWork是HandlerThread实现的单线程。
            SharedPreferencesImpl.this.enqueueDiskWrite(
                mcr, null /* sync write on this thread okay */);
            try {
				//阻塞当前线程，直到写入文件完成
                mcr.writtenToDiskLatch.await();
            } catch (InterruptedException e) {
                return false;
            } finally {
                if (DEBUG) {
                    Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                            + " committed after " + (System.currentTimeMillis() - startTime)
                            + " ms");
                }
            }
            notifyListeners(mcr);
			//3 返回是否写入成功
            return mcr.writeToDiskResult;
        }

		//注释1 把mModefied的里面的值放到mMap里面
	  private MemoryCommitResult commitToMemory() {
           ...
                synchronized (mLock) {
                    boolean changesMade = false;
					//如果调用了clear方法。则mClear为true.
                    if (mClear) {
                        if (!mMap.isEmpty()) {
                            changesMade = true;
                            mMap.clear();
                        }
                        mClear = false;
                    }
					//把mModefied的里面的值放到mMap里面。mModefied是当前Editor调用putXXX的集合。mMap则是所有数据的集合。
                    for (Map.Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
              
                        if (v == this || v == null) {
                            if (!mMap.containsKey(k)) {
                                continue;
                            }
                            mMap.remove(k);
                        } else {
                            if (mMap.containsKey(k)) {
                                Object existingValue = mMap.get(k);
                                if (existingValue != null && existingValue.equals(v)) {
                                    continue;
                                }
                            }
                            mMap.put(k, v);
                        }

                        changesMade = true;
                        if (hasListeners) {
                            keysModified.add(k);
                        }
                    }

                    mModified.clear();
                    if (changesMade) {
                        mCurrentMemoryStateGeneration++;
                    }
                    memoryStateGeneration = mCurrentMemoryStateGeneration;
                }
            }
            return new MemoryCommitResult(memoryStateGeneration, keysModified, listeners,
                    mapToWriteToDisk);
        }

	//注释2 最终会调用如下方法 写入文件,并且写入结果在MemoryCommitResult对象
	 private void writeToFile(MemoryCommitResult mcr, boolean isFromSyncCommit) {
	       ...
	        // Attempt to write the file, delete the backup and return true as atomically as
	        // possible.  If any exception occurs, delete the new file; next time we will restore
	        // from the backup.
	        try {
				//获取输入流
	            FileOutputStream str = createFileOutputStream(mFile);
   				//同样用Pull写入
	            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
	
	            writeTime = System.currentTimeMillis();
				//刷新
	            FileUtils.sync(str);
	
	            fsyncTime = System.currentTimeMillis();
				//关闭流
	            str.close();
	    
				
				//写入成功了。释放
	            mcr.setDiskWriteResult(true, true);
	            long fsyncDuration = fsyncTime - writeTime;
	            mSyncTimes.add((int) fsyncDuration);
	            mNumSync++;
				//返回。
	            return;
	        } catch (XmlPullParserException e) {
	            Log.w(TAG, "writeToFile: Got exception:", e);
	        } catch (IOException e) {
	            Log.w(TAG, "writeToFile: Got exception:", e);
	        }
			//如果写入失败。释放
	        mcr.setDiskWriteResult(false, false);
	    }
小结：`commit`方法运行在当前调用的线程中。如果未写入文件，则阻塞当前进程等待写入文件数据完成才会返回。

###apply方法

        public void apply() {
            final long startTime = System.currentTimeMillis();
			//把mModefied的里面的值放到mMap里面
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                    public void run() {
                        try {
							//阻塞
                            mcr.writtenToDiskLatch.await();
                        } catch (InterruptedException ignored) {
                        }

                        if (DEBUG && mcr.wasWritten) {
                            Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                                    + " applied after " + (System.currentTimeMillis() - startTime)
                                    + " ms");
                        }
                    }
                };
			//添加到sFinishers当中
            QueuedWork.addFinisher(awaitCommit);

            Runnable postWriteRunnable = new Runnable() {
                    public void run() {
						
                        awaitCommit.run();
                        QueuedWork.removeFinisher(awaitCommit);
                    }
                };
			//放入QueuedWork队列
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);

            // Okay to notify the listeners before it's hit disk
            // because the listeners should always get the same
            // SharedPreferences instance back, which has the
            // changes reflected in memory.
            notifyListeners(mcr);
        }


	//QueuedWork.queue先执行writeToDiskRunnable里面的run方法。而后执行postWriteRunnable里面的run方法
	  private void enqueueDiskWrite(final MemoryCommitResult mcr,
	                                  final Runnable postWriteRunnable) {
	        final boolean isFromSyncCommit = (postWriteRunnable == null);
	
	        final Runnable writeToDiskRunnable = new Runnable() {
	                public void run() {
	                    synchronized (mWritingToDiskLock) {
							//1 执行完写入文件的操作
	                        writeToFile(mcr, isFromSyncCommit);
	                    }
	                    synchronized (mLock) {
	                        mDiskWritesInFlight--;
	                    }
						//2 执行postWriteRunnable的方法
	                    if (postWriteRunnable != null) {
	                        postWriteRunnable.run();
	                    }
	                }
	            };
	
	        // Typical #commit() path with fewer allocations, doing a write on
	        // the current thread.
	        if (isFromSyncCommit) {
	            boolean wasEmpty = false;
	            synchronized (mLock) {
	                wasEmpty = mDiskWritesInFlight == 1;
	            }
	            if (wasEmpty) {
	                writeToDiskRunnable.run();
	                return;
	            }
	        }
	
	        QueuedWork.queue(writeToDiskRunnable, !isFromSyncCommit);
	    }

小结:`apply`方法放入列队中等待执行，不会影响当前线程

在`apply`里面看到，添加了一个阻塞的任务到`QueuedWork`的`sFinishers`里面，

	   public void apply() {
            final long startTime = System.currentTimeMillis();
			//把mModefied的里面的值放到mMap里面
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                    public void run() {
                        try {
							//阻塞
                            mcr.writtenToDiskLatch.await();
                        } catch (InterruptedException ignored) {
                        }

                        if (DEBUG && mcr.wasWritten) {
                            Log.d(TAG, mFile.getName() + ":" + mcr.memoryStateGeneration
                                    + " applied after " + (System.currentTimeMillis() - startTime)
                                    + " ms");
                        }
                    }
                };
			//添加到sFinishers当中
            QueuedWork.addFinisher(awaitCommit);

         ...
        }


而`QueueWork`里面还有个触发`sFinishers`执行的，这个方法在哪里执行的呢

	public static void waitToFinish() {
        long startTime = System.currentTimeMillis();
        boolean hadMessages = false;

        Handler handler = getHandler();

        synchronized (sLock) {
            if (handler.hasMessages(QueuedWorkHandler.MSG_RUN)) {
                // Delayed work will be processed at processPendingWork() below
                handler.removeMessages(QueuedWorkHandler.MSG_RUN);

                if (DEBUG) {
                    hadMessages = true;
                    Log.d(LOG_TAG, "waiting");
                }
            }

            // We should not delay any work as this might delay the finishers
            sCanDelay = false;
        }

        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            processPendingWork();
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }

        try {
            while (true) {
                Runnable finisher;

                synchronized (sLock) {
                    finisher = sFinishers.poll();
                }

                if (finisher == null) {
                    break;
                }

                finisher.run();
            }
        } finally {
            sCanDelay = true;
        }

        synchronized (sLock) {
            long waitTime = System.currentTimeMillis() - startTime;

            if (waitTime > 0 || hadMessages) {
                mWaitTimes.add(Long.valueOf(waitTime).intValue());
                mNumWaits++;

                if (DEBUG || mNumWaits % 1024 == 0 || waitTime > MAX_WAIT_TIME_MILLIS) {
                    mWaitTimes.log(LOG_TAG, "waited: ");
                }
            }
        }
    }

`Service`和,`Activity`的生命周期方法里面会调用。为什么会在`Service`和`Activity`的生命周期里面调用这些方法？如果耗时操作岂不会阻塞主线程？因为都在stop或者pause和stop里面执行了。可能是保证内存中的数据存入文件中。

 	private void handleStopService(IBinder token) {
        ...
                QueuedWork.waitToFinish();
                try {
                    ActivityManager.getService().serviceDoneExecuting(
                            token, SERVICE_DONE_EXECUTING_STOP, 0, 0);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
         ...
    }
	 private void handlePauseActivity(IBinder token, boolean finished,
            boolean userLeaving, int configChanges, boolean dontReport, int seq) {
        ActivityClientRecord r = mActivities.get(token);
       ...
                QueuedWork.waitToFinish();
            }

            // Tell the activity manager we have paused.
            if (!dontReport) {
                try {
                    ActivityManager.getService().activityPaused(token);
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
           ...
    }

###总结:commit方法运行在当前调用者的线程，并且一直阻塞等待结果返回。如果数据大且在主线程可能导致ANR。
###apply方法 不会阻塞当前线程。而是在QueueWork的单线程里面执行。但是Service，和Activity的stop里面会执行QueueWork里面的waitToFinish方法。等待applay里面的数据写入文件。因此也可能发送ANR
