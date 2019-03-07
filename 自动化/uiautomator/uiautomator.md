##Android自动化测试原来可以这么简单

先上效果图


![](https://i.imgur.com/Hm5zKy8.gif)






主要代码如下，可以看到代码很清晰明了。主要是基于uiautomator 二次封装。因此代码写起来清爽明亮。这里主要是调用封装后的代码，所以要了解uiautomator的方法还是要看封装的代码如何调用的。

	 private void testLongsit(){
			//点击
	        click("il_remind_sport");
			//截图
	        screenShot("久坐界面");
	
	        //打开开关
	        clickNow("toggle");
	
	        //左或者右滑动
	        randomSwipLeftOrRight("remind_sport_interval",3);
	
	        //设置开始时间
	        click("remind_sport_start",DIALOG_dURATION);
			//上或者下滑动
	        randomSwipUpOrDown("hour",3);
	        randomSwipUpOrDown("min",3);
	
	        screenShot("设置开始时间");
	        click("setTv",DIALOG_dURATION);
	
	        //设置结束时间
	        click("remind_sport_end",DIALOG_dURATION);
	
	        randomSwipUpOrDown("hour",3);
	        randomSwipUpOrDown("min",3);
	        screenShot("设置结束时间");
	        click("setTv",DIALOG_dURATION);
	
	
	        clickNow("week_day3");
	
	        clickNow("week_day5");
	
	        screenShot("久坐界面设置");
	        clickSet();
	
	        click("il_remind_sport");
	
	        screenShot("久坐界面完成");
	
	
	        clickBack();
	    }
  
要用uiautomator，首先要添加配置和依赖才能用。



添加配置

 	defaultConfig {
       ...
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }

添加依赖

 	//测试框架

    androidTestCompile 'com.android.support.test:runner:0.5' ;
    androidTestCompile 'com.android.support.test.uiautomator:uiautomator-v18:2.1.1'


主要封装代码，这个封装代码也是以前当时准备写UI自动化的时候，网上看到。这里对uiautomator进行了基本的封装。



		public class UiaLibrary extends UiAutomatorTestCase {
		
			public String screenshotDir="/sdcard/auitest/";
		
			public   String PACKAGE="com.veryfit2hr.second";
			public   String ID_PRE=PACKAGE+":id/";
			public String FIRST_ACTIVITY="com.ido.veryfitpro.module.bind.WelcomeActivity";
			public static final int CLICK_DELAY = 300;
			public static final int LAUNCH_TIMEOUT = 5000;
			public static final int NET_WORK_DELAY=2000;
			public static final int MAIN_ANIMTOR_DELAY=3000;
			public static final int DIALOG_dURATION=600;
			public static final int CLICK_NOW=100;
			public  String dirName;
			public final static String TAG="UI_TEST_";
			//在屏幕上滑动
			public void swipeLeft() {//左滑
				int y = UiDevice.getInstance().getDisplayHeight();
				int x = UiDevice.getInstance().getDisplayWidth();
				UiDevice.getInstance().swipe(x - 100, y / 2, 100, y / 2, 20);
				sleep(150);
			}
		
			public void swipeRight() {//右滑
				int y = UiDevice.getInstance().getDisplayHeight();
				int x = UiDevice.getInstance().getDisplayWidth();
				UiDevice.getInstance().swipe(100, y / 2, x - 100, y / 2, 20);
				sleep(150);
			}
		
			public void swipeDown() {//下滑
				int y = UiDevice.getInstance().getDisplayHeight();
				int x = UiDevice.getInstance().getDisplayWidth();
				UiDevice.getInstance().swipe(x / 2, 200, x / 2, y - 200, 20);
				sleep(150);
			}
		
			public void swipeUp() {//上滑
				int y = UiDevice.getInstance().getDisplayHeight();
				int x = UiDevice.getInstance().getDisplayWidth();
				UiDevice.getInstance().swipe(x / 2, y - 200, x / 2, 200, 20);
				sleep(150);
			}
		
			public void swipUpLittle() {//上滑一点点
				int x = UiDevice.getInstance().getDisplayWidth() / 2;
				int y = UiDevice.getInstance().getDisplayHeight() / 2;
				UiDevice.getInstance().swipe(x, y + 150, x, y - 150, 20);
				sleep(150);
			}
		
			public void swipDownLittle() {//下拉一点点
				int x = UiDevice.getInstance().getDisplayWidth() / 2;
				int y = UiDevice.getInstance().getDisplayHeight() / 2;
				UiDevice.getInstance().swipe(x, y - 150, x, y + 150, 20);
				sleep(150);
			}
		
			public String getNow() {//获取当前时间
				Date time = new Date();
				SimpleDateFormat now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String c = now.format(time);
				return c;
			}
			public String screenShotByDir(String name) {//截图并命名
				return screenShot(name,dirName);
			}
		
			/**
			 * 如果dirName存在，则截图在dirName目标下，否则截图在screenshotDir目标下
			 * @param name
			 * @return
			 */
			public String screenShot(String name) {//截图并命名
				LogUtil.d2(TAG+name);
				if (!TextUtils.isEmpty(dirName)){
					return screenShotByDir(name);
				}
				return screenShot(name,null);
			}
			public String screenShot(String name,String dir) {//截图并命名
				File file ;
				if (TextUtils.isEmpty(dir)){
					file = new File(screenshotDir);
				}else{
					file = new File(screenshotDir+dir);
				}
		
				if (!file.exists()) {
					file.mkdirs();
				}
				File files = new File(file , name + ".png");
				if (files.exists()){
					files.delete();
				}
				UiDevice.getInstance().takeScreenshot(files);
				output(name + ".png 截图成功！");
				String path = files.getAbsolutePath();
				return path;
			}
			//通过文本获取控件
			public UiObject getUiObjectByText(String text) {
				return new UiObject(new UiSelector().text(text));
			}
			//通过文本获取控件
			public UiObject getUiObjectByTextContains(String text) {
				return new UiObject(new UiSelector().textContains(text));
			}
			//通过id获取控件
			public UiObject getOjectByResourceId(String resourceId){
				UiObject uiObject=new UiObject(new UiSelector().resourceId(ID_PRE+resourceId));
				if (!uiObject.exists()){
					output("resourceId cannot find ["+ID_PRE+resourceId+"]");
				}
		
				return uiObject;
			}
			public UiObject getUiObject(String res){
		
				return getUiObject(res,true);
			}
			public UiObject getUiObject(String res,boolean isAssertExist){
				UiObject oneButton = getUiObjectByTextOrDescription(res);
				if (!oneButton.exists()){
					output("getUiObjectByTextOrDescription cannot find ["+res+"]");
					oneButton=getOjectByResourceId(res);
				}
				if (isAssertExist) {
					Assert.assertEquals(oneButton.exists(), true);
				}
				return oneButton;
			}
			public UiObject getChild(String parent,String child){
				UiObject gridview=getUiObject(parent);
				UiObject uiObject=null;
				try {
					uiObject= gridview.getChild(new UiSelector().text(child));
					if (!uiObject.exists()){
						uiObject=gridview.getChild(new UiSelector().description(child));
					}
				} catch (UiObjectNotFoundException e) {
					e.printStackTrace();
				}
				return uiObject;
			}
			public UiObject getUiObjectWaitIfExist(String res,int waitTime){
				UiObject oneButton = getUiObjectByTextOrDescription(res);
				if (!oneButton.exists()){
					oneButton=getOjectByResourceId(res);
				}
				if (!oneButton.exists()){
					oneButton.waitForExists(waitTime);
				}
				return oneButton;
			}
			public UiObject getUiObjectByTextWaitIfExist(String res,int waitTime){
				UiObject oneButton = getUiObjectByText(res);
				if (!oneButton.exists()){
					oneButton.waitForExists(waitTime);
				}
				return oneButton;
			}
		
			public void swipeLeft(String resourceId,int step){
				UiObject uiObject=getUiObject(resourceId);
				try {
					output("["+resourceId+"]"+"isScrollable:"+uiObject.isScrollable());
					uiObject.swipeLeft(step);
				} catch (UiObjectNotFoundException e) {
		
					e.printStackTrace();
				}
				sleep(CLICK_DELAY);
			}
			//通过text开始文字查找控件
			public UiObject getUiObjectByStartText(String text) {
				return new UiObject(new UiSelector().textStartsWith(text));
			}
			public UiObject getUiObjectByTextClassName(String text, String classname) {//通过文本和类名获取控件
				return new UiObject(new UiSelector().text(text).className(classname));
			}
		
			public UiObject getUiObjectByTextResourceId(String text, String id) {//通过文本和id获取对象
				return new UiObject(new UiSelector().text(text).resourceId(id));
			}
		
			public UiObject getUiObjectByResourceIdClassName(String id, String type) {
				return new UiObject(new UiSelector().resourceId(id).className(type));
			}
			public UiObject getUiObjectByTextOrDescription(String text) {
				UiObject uiObject = new UiObject(new UiSelector().text(text));
				boolean textExist=uiObject.exists();
				if (!textExist){
					uiObject = new UiObject(new UiSelector().description(text));
				}
				return uiObject;
			}
		
			public UiObject getUiObjectByResourceId(String id) {//通过资源ID获取控件
				return getOjectByResourceId(id);
			}
		
			public UiObject getUiObjectByDesc(String desc) {//通过desc获取控件
				return new UiObject(new UiSelector().description(desc));
			}
		
			public UiObject getUiObjectByStartDescContains(String desc) {
				return new UiObject(new UiSelector().descriptionContains(desc));
			}
		
			public UiObject getUiObjectByDescContains(String desc) {
				return new UiObject(new UiSelector().descriptionContains(desc));
			}
		
			public UiObject getUiObjectByClassName(String type) {//通过classname获取控件
				return new UiObject(new UiSelector().className(type));
			}
		
			public UiObject getUiObjectByResourceIdIntance(String id, int instance) {//通过id和instance获取控件
				return new UiObject(new UiSelector().resourceId(id).instance(instance));
			}
		
			//长按控件
			public void longclickUiObectByResourceId(String id) throws UiObjectNotFoundException {
				int x = getUiObjectByResourceId(id).getBounds().centerX();
				int y = getUiObjectByResourceId(id).getBounds().centerY();
				UiDevice.getInstance().swipe(x, y, x, y, 300);//最后一个参数单位是5ms
			}
		
			public void longclickUiObectByDesc(String desc) throws UiObjectNotFoundException {
				int x = getUiObjectByDesc(desc).getBounds().centerX();
				int y = getUiObjectByDesc(desc).getBounds().centerY();
				UiDevice.getInstance().swipe(x, y, x, y, 300);//最后一个参数单位是5ms
			}
		
			public void longclickUiObectByText(String text) throws UiObjectNotFoundException {
				int x = getUiObjectByText(text).getBounds().centerX();
				int y = getUiObjectByText(text).getBounds().centerY();
				UiDevice.getInstance().swipe(x, y, x, y, 300);//最后一个参数单位是5ms
			}
		
			//点击中心
			public void clickCenter() {
				int x = UiDevice.getInstance().getDisplayWidth();
				int y = UiDevice.getInstance().getDisplayHeight();
				clickPiont(x / 2, y / 2);
			}
		
			public void writeText(String text) throws UiObjectNotFoundException {//输入文字
			//		getUiObjectByClassName("android.widget.EditText").setText(Utf7ImeHelper.e(text));
			}
		
			public UiScrollable getUiScrollabe() {//获取滚动控件
				return new UiScrollable(new UiSelector().scrollable(true));
			}
		
			public UiScrollable getUiScrollableByResourceId(String id) {//获取滚动对象
				return new UiScrollable(new UiSelector().scrollable(true).resourceId(id));
			}
		
			public void getChildByTextOfUiScrollableByClassName(String type, String text) throws UiObjectNotFoundException {
				getScrollableByClassName(type).getChildByText(new UiSelector().text(text), text).clickAndWaitForNewWindow();
			}
		
			public UiObject getUiObjectByResourIdIndex(String id, int index) {//通过ID和index获取控件
				return new UiObject(new UiSelector().resourceId(id).index(index));
			}
		
			public void randomClickOpiton() throws UiObjectNotFoundException {
				int num = getUiObjectByClassName("android.widget.ListView").getChildCount();
				int i = new Random().nextInt(num);
				getUiObjectByResourceIdIntance("com.gaotu100.superclass:id/simpleitemview_left_text", i).clickAndWaitForNewWindow();
			}
		
			public void outputBegin(String text) {//输出开始
				LogUtil.d2(TAG+text + "..-. ...- 测试开始！");
			}
		
			public void outputNow() {//输出当前时间
				System.out.println(getNow());
			}
		
			public void outputOver(String text) {//输出结束
				LogUtil.d2(TAG+text + "..-. ...- 测试结束！");
			}
			private static SimpleDateFormat myLogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			//明显输出
			public void output(String text) {
				writeLog(text);
				LogUtil.d2(TAG+text);
			}
			//明显输出
			public void outputStart(int index,String text) {
				StringBuffer stringBuffer=new StringBuffer();
				stringBuffer.append(TAG);
				for (int i=1;i<index;i++){
					stringBuffer.append(" ");
				}
				for (int i=0;i<10-index;i++){
					stringBuffer.append("*");
				}
				stringBuffer.append(text);
				stringBuffer.append("开始");
				for (int i=0;i<10-index;i++){
					stringBuffer.append("*");
				}
				writeLog(stringBuffer.toString());
				LogUtil.d2(stringBuffer.toString());
			}
			private static boolean writeStringToFile(String path, String data){
				boolean isSuccess = true;
				File file = new File(path);
				if (!file.exists()){
					try {
						isSuccess = file.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
						isSuccess = false;
					}
				}
				if (isSuccess) {
					try {
						FileWriter writer = new FileWriter(path,true);
						writer.write(data);
						writer.write("\n");
						writer.flush();
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
						isSuccess = false;
					}
				}
		
				return isSuccess;
			}
			public void writeLog(String log){
				log +="["+myLogSdf.format(new Date())+"]"+log;
				writeStringToFile(new File(screenshotDir,"ui_result.txt").getAbsolutePath(),log);
			}
			void deleteLogFile(){
				new File(screenshotDir,"ui_result.txt").delete();
			}
			//明显输出
			public void outputEnd(int index,String text) {
				StringBuffer stringBuffer=new StringBuffer();
				stringBuffer.append(TAG);
				for (int i=1;i<index;i++){
					stringBuffer.append(" ");
				}
				for (int i=0;i<5-index;i++){
					stringBuffer.append("*");
				}
				stringBuffer.append(text);
				stringBuffer.append("结束");
				writeLog(stringBuffer.toString());
				LogUtil.d2(stringBuffer.toString());
			}
		
		
			public void output(int... num) {//方法重载
				for (int i = 0; i < num.length; i++) {
					LogUtil.d2("第" + (i + 1) + "个：" + num[i]);
				}
			}
		
			public void output(Object... object) {
				for (int i = 0; i < object.length; i++) {
					LogUtil.d2("第" + (i + 1) + "个：" + object[i]);
				}
			}
		
			public void output(Object object) {
				LogUtil.d2(TAG+object.toString());
			}
		
			public void pressTimes(int keyCode, int times) {//对于一个按键按多次
				for (int i = 0; i < times; i++) {
					sleep(200);
					UiDevice.getInstance().pressKeyCode(keyCode);
				}
			}
			public void waitForUiObjectByStartText(String text) {
				getUiObjectByStartText(text).waitForExists(10000);
			}
		
			//输出时间差
			public void outputTimeDiffer(Date start, Date end) {
				long time = end.getTime() - start.getTime();
				double differ = (double) time / 1000;
				output("总计用时" + differ + "秒！");
			}
		
			//获取子控件点击
			public void getScrollChildByText(String text) throws UiObjectNotFoundException {
				UiObject child = getUiScrollabe().getChildByText(new UiSelector().text(text), text);
				child.clickAndWaitForNewWindow();
			}
		
			//通过classname获取滚动控件
			public UiScrollable getScrollableByClassName(String type) {
				return new UiScrollable(new UiSelector().scrollable(true).className(type));
			}
		
			public void waitForUiObjectByClassName(String type) throws UiObjectNotFoundException {//等待控件出现
				getUiObjectByClassName(type).waitForExists(10000);
			}
		
			public String getTextByResourceId(String id) throws UiObjectNotFoundException {
				return getUiObjectByResourceId(id).getText();
			}
		
			public String getDescByResourceI1d(String id) throws UiObjectNotFoundException {
				return getUiObjectByResourceId(id).getContentDescription();
			}
		
			public String getTextByResourceIdClassName(String id, String type) throws UiObjectNotFoundException {
				return getUiObjectByResourceIdClassName(id, type).getText();
			}
		
			//获取兄弟控件的文本
			public String getTextByBrother(String myid, String brotherid) throws UiObjectNotFoundException {
				return getUiObjectByResourceId(myid).getFromParent(new UiSelector().resourceId(brotherid)).getText();
			}
		
			public void clickPiont(int x, int y) {//点击某一个点
				UiDevice.getInstance().click(x, y);
			}
		
			//等待文本控件并点击
			public void waitForClassNameAndClick(String type) throws UiObjectNotFoundException {
				waitForUiObjectByClassName(type);
				getUiObjectByClassName(type).clickAndWaitForNewWindow();
			}
		
			public void waitForTextAndClick(String text) throws UiObjectNotFoundException {
				waitForUiObjectByText(text);
		//		getUiObjectByText(text).waitForExists(10000);
				getUiObjectByText(text).clickAndWaitForNewWindow();
			}
			
		
			//向前滚动
			public boolean scrollForward() throws UiObjectNotFoundException {
				return getUiScrollabe().scrollForward(50);
			}
		
			//向后滚动
			public boolean scrollBackward() throws UiObjectNotFoundException {
				return getUiScrollabe().scrollBackward(50);
			}
		
			public void deleteScreenShot() {//删除截图文件夹
				File file = new File("/mnt/sdcard/123/");
				if (file.exists()) {//如果file存在
					File[] files = file.listFiles();//获取文件夹下文件列表
					for (int i = 0; i < files.length; i++) {//遍历删除
						files[i].delete();
					}
					file.delete();//最后删除文件夹，如果不存在直接删除文件夹
				} else {
					output("文件夹不存在！");
				}
		
			}
			public void pressBack(){
				UiDevice.getInstance().pressBack();
			}
			public String formatTime(long time){
				return String.format("%02dmin %02ds",time/1000/60,time/1000%60);
			}
			}
后面我基于它再进行了封装，更适合自己的app编写测试代码。

	public class BaseUiAbrary extends UiaLibrary {
	    public static String titleRightId = "layout_right";
	    public static String titleLeftId = "layout_left";
	
	
	    public void clickSet() {
	        click(titleRightId, 1000 + CLICK_DELAY);
	    }
	
	    public void clickBack() {
	        click(titleLeftId);
	    }
	
	    /**
	     * 删除dirName目录下面的文件
	     */
	    public void deleteScreenShotByDir() {//截图并命名
	        deleteScreenShotFile(dirName);
	    }
	
	    public boolean random() {
	        Random random = new Random();
	        if (random.nextInt(11) % 2 == 0) {
	            return true;
	        }
	        return false;
	    }
	
	    public void clickScreen() {
	        UiDevice.getInstance().click(100, 100);
	    }
	
	    public void clickListViewByResourceId(String id) {
	        UiObject listView = getOjectByResourceId(id);
	        Rect rect = null;
	        try {
	            rect = listView.getBounds();
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	        UiDevice.getInstance().click(rect.left + 100, rect.top + 100);
	    }
	    public void clickListView(Class clazz, Class childClazz,int index){
	        UiScrollable functionItems = new UiScrollable(new UiSelector().className(clazz));
	        Assert.assertEquals(functionItems.exists(), true);
	        try {
	            functionItems.getChildByInstance(new UiSelector().className(childClazz),index);
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	    }
	    public void clickListViewByResourceId(String id, int offLeft, int offTop) {
	        UiObject listView = getOjectByResourceId(id);
	        Rect rect = null;
	        try {
	            rect = listView.getBounds();
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	        UiDevice.getInstance().click(rect.left + offLeft, rect.top + offTop);
	    }
		//点击列表控件
	    public void clickScrollable(Class clazz, Class childClazz, String text) {
	
	        UiScrollable functionItems = new UiScrollable(new UiSelector().className(clazz));
	        Assert.assertEquals(functionItems.exists(), true);
	        UiObject apps = null;
	        try {
	            apps = functionItems.getChildByText(new UiSelector().className(childClazz), text);
	            apps.click();
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	            Assert.assertEquals(false, true);
	
	        }
	    }
	    public void longClickScrollable(Class clazz, Class childClazz, String text) {
	
	        UiScrollable functionItems = new UiScrollable(new UiSelector().className(clazz));
	        Assert.assertEquals(functionItems.exists(), true);
	        UiObject apps = null;
	        try {
	            apps = functionItems.getChildByText(new UiSelector().className(childClazz), text);
	            apps.longClick();
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	            Assert.assertEquals(false, true);
	
	        }
	    }
	
	    public void clickWaitExist(String text) {
	        UiObject bind = getUiObjectByText(text);
	        bind.waitForExists(LAUNCH_TIMEOUT);
	        try {
	            bind.click();
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	    }
	
	    /**
	     * 设置text字符串
	     *
	     * @param text
	     * @param content
	     */
	    public void setText(String text, String content) {
	        try {
	            getUiObject(text).setText(content);
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	    }
	
	    /**
	     * 设置text字符串
	     *
	     * @param id
	     * @param content
	     */
	    public void setTextById(String id, String content) {
	        try {
	            getUiObject(id).setText(content);
	        } catch (UiObjectNotFoundException e) {
	            e.printStackTrace();
	        }
	    }
	
	    /**
	     * 如果控件存在就点击。
	     *
	     * @param id
	     */
	    public void clickIfViewExist(String id) {
	        clickIfViewExist(id, CLICK_DELAY);
	    }
	
	    /**
	     * 如果控件存在就点击。
	     *
	     * @param id
	     */
	    public void clickIfViewExist(String id, int delay) {
	        UiObject uiObject = getUiObject(id, false);
	        if (uiObject.exists()) {
	            click(id, delay);
	        }
	    }
	
	    /**
	     * 如果控件存在就点击。
	     * 不存在就跳过
	     * 试用于系统弹出框
	     *
	     * @param text
	     */
	    public void clickIfViewExistByText(String text) {
	        clickIfViewExist(text);
	    }
	
	    public void clickByResourceId(String resourceId) {
	        try {
	            UiObject oneButton = getOjectByResourceId(resourceId);
	            oneButton.click();
	            sleep(CLICK_DELAY);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	
	    /**
	     * 根据text或者desctiption或者id去查找控件
	     *
	     * @param text
	     */
	    public void click(String text) {
	        click(text, CLICK_DELAY);
	    }
	
	    /**
	     * 根据text或者desctiption或者id去查找控件
	     *
	     * @param text
	     */
	    public void clickWaitExist(String text, boolean time) {
	        click(text, CLICK_DELAY);
	    }
	
	    /**
	     * 根据text或者desctiption或者id去查找控件
	     *
	     * @param text
	     */
	    public void clickNow(String text) {
	        click(text, CLICK_NOW);
	    }
	
	    /**
	     * 根据text或者desctiption或者id去查找控件
	     *
	     * @param text
	     */
	    public void click(String text, int clickDelay) {
	        try {
	            UiObject oneButton = getUiObject(text);
	            oneButton.click();
	            sleep(clickDelay);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	
	    public void clickAndWaitForNewWindow(String text) {
	        try {
	            UiObject oneButton = getUiObjectByTextOrDescription(text);
	            oneButton.clickAndWaitForNewWindow();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	
	    /**
	     * 随机左滑或者右滑
	     *
	     * @param resourceId
	     * @param step
	     */
	    public void randomSwipLeftOrRight(String resourceId, int step) {
	        if (random()) {
	            swipeLeft(resourceId, step);
	        } else {
	            swipeRight(resourceId, step);
	        }
	    }
	
	    /**
	     * 随机上滑或者下滑
	     *
	     * @param resourceId
	     * @param step
	     */
	    public void randomSwipUpOrDown(String resourceId, int step) {
	        if (random()) {
	            //往下划
	            swipeDown(resourceId, step);
	        } else {
	            swipeUp(resourceId, step);
	        }
	    }
	
	    public void swipeUp(String resourceId, int step) {
	        UiObject uiObject = getUiObject(resourceId);
	        try {
	            uiObject.swipeUp(step);
	        } catch (UiObjectNotFoundException e) {
	            output("[" + resourceId + "] not found");
	            e.printStackTrace();
	            assertTrue("[" + resourceId + "] not found", false);
	        }
	        sleep(CLICK_DELAY);
	    }
	
	    public void swipeDown(String resourceId, int step) {
	        UiObject uiObject = getUiObject(resourceId);
	
	        try {
	            uiObject.swipeDown(step);
	        } catch (UiObjectNotFoundException e) {
	            output("[" + resourceId + "] not found");
	            e.printStackTrace();
	            assertTrue("[" + resourceId + "] not found", false);
	        }
	        sleep(CLICK_DELAY);
	    }
	
	    public void swipeDown2(String resourceId, int step) {
	        UiObject uiObject = getUiObject(resourceId);
	
	        try {
	//            InstrumentationUiAutomatorBridge bridge= (InstrumentationUiAutomatorBridge) ReflectUtil.invokeMethod(UiDevice.getInstance(),"getAutomatorBridge",new Class[]{});
	
	//            UiDevice.getInstance().drag(1500,1500,1800,1800,3);
	//            UiDevice.getInstance().swipe(500,500,800,800,3);
	            uiObject.swipeDown(step);
	        } catch (Exception e) {
	            assertTrue("[" + resourceId + "] not found", false);
	            e.printStackTrace();
	        }
	        sleep(CLICK_DELAY);
	    }
	
	    public void swipeRight(String resourceId, int step) {
	        UiObject uiObject = getUiObject(resourceId);
	
	        try {
	            output("[" + resourceId + "]" + "isScrollable:" + uiObject.isScrollable());
	            uiObject.swipeRight(step);
	        } catch (UiObjectNotFoundException e) {
	            assertTrue("[" + resourceId + "] not found", false);
	            e.printStackTrace();
	        }
	        sleep(CLICK_DELAY);
	    }
	
	    /**
	     * 如果dirName为空，删除dirName目录下的文件。如果不为空，删除screenshotDir目录下的文件
	     */
	    public void deleteScreenShotFile() {
	        if (!TextUtils.isEmpty(dirName)) {
	            deleteScreenShotFile(dirName);
	            return;
	        }
	        File file = new File(screenshotDir);
	        if (!file.exists()) {
	            return;
	        }
	        if (file.isFile()) {
	            return;
	        }
	        File[] files = file.listFiles();
	        if (files != null && file.length() > 0) {
	
	            for (File file1 : files) {
	                if (file1.isFile()) {
	                    file1.delete();
	                }
	
	            }
	
	        }
	    }
	
	    public void deleteScreenShotFile(String dir) {
	        File file = new File(screenshotDir + dir);
	        if (!file.exists()) {
	            return;
	        }
	        if (file.isFile()) {
	            return;
	        }
	        File[] files = file.listFiles();
	        if (files != null && file.length() > 0) {
	
	            for (File file1 : files) {
	                file1.delete();
	
	            }
	
	        }
	    }
	
	}


 然后再封装一个所有模块都要做的，比如当前测试的模块名称，截图存在当前模块目录下，计算测试当前模块耗时时长。以及当前模块是否需要测试。

	public abstract class BaseTest extends BaseUiAbrary{
	     protected String modelName="";
	     public BaseTest(){
	
	     }
	
	     public void testMain(){
	          //获取注解
	          Class<KeepTest> clazz=KeepTest.class;
	          KeepTest annotation=getClass().getAnnotation(clazz);
	          if (annotation!=null){
	              boolean isTest=annotation.isTest();
	              if (!isTest){
	                   output("*************"+modelName+"模块 不测试!!!!!");
	                   return;
	              }else{
	                   output("*************"+modelName+"模块 需要测试!!!!!");
	              }
	          }
	
	          deleteScreenShotByDir();
	          long start=System.currentTimeMillis();
	          outputStart(2,"测试"+modelName+"模块");
	          realTestMain();
	          outputEnd(2,"测试"+modelName+"模块");
	          long end=System.currentTimeMillis();
	          output("*************测试"+modelName+"模块耗时:"+formatTime(end-start)+"*************************");
	     };
	     public abstract void realTestMain();
	}

这里贴某一个模块的某一部分代码


	KeepTest(isTest = true)
	public class HomeDeviceTest extends BaseTest {
	    SupportFunctionInfo supportFunctionInfo;
	    public HomeDeviceTest(){
	        dirName="device";
	        modelName="设备";
	    }
	
	
	    @Override
	    public void realTestMain() {
	        supportFunctionInfo = LocalDataManager.getSupportFunctionInfo();
	        //点击设备
	        click("rb_tab_device");
	        screenShot("设备界面");
	        if (!BLEManager.isConnected() || DeviceSynchPresenter.getInstance().isSynchDataIng()) {
	            sleep(5000);
	            return;
				...
	        }
	}

有了之前的封装，以后写测试代码就贼轻松了。



