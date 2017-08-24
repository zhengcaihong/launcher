package com.yunosauto.testsuites.homeshell;

import java.awt.Rectangle;

import com.aliyun.framework.log.SysLog;
import com.yaf.framework.Assert;
import com.yaf.framework.core.Sleep;
import com.yaf.framework.interfaces.vo.UiObject;
import com.yaf.framework.interfaces.vo.UiSelector;
import com.yaf.framework.interfaces.wrapper.Keys;
import com.yaf.framework.interfaces.wrapper.PackageBox;
import com.yaf.framework.interfaces.wrapper.PixelPerfBox;
import com.yaf.framework.interfaces.wrapper.ResponseTimeBox;
import com.yaf.framework.interfaces.wrapper.ScreenBox;
import com.yaf.framework.interfaces.wrapper.SystemInfoBox;
import com.yaf.framework.interfaces.wrapper.UiBox;
import com.yaf.framework.interfaces.wrapper.Waiter;

public class CommonOperation {
	
	public static final int  HomeX = 80; //HOME鍵的X坐标
	public static final int HomeY = 1030; //HOME鍵的Y坐标
	public static final int BackX = 0x00000015e; //HOME鍵的Y坐标
	public static final int BackY = 0x000000406; //HOME鍵的Y坐标
	//3.0的坐标
		public static final int BackX1 = 0x00000370; //Back鍵的X坐标
		public static final int BackY1 = 0x00000834; //Back键的Y坐标
		public static final int  HomeX1 = 0x0000021c; //HOME鍵的X坐标
		public static final int HomeY1 = 0x00000834; //HOME鍵的Y坐标
	public static String CameraActivity = "com.yunos.camera/com.yunos.camera.CameraActivity";
	public static String GalleryActivity = "com.aliyun.image/com.aliyun.image.app.Gallery";
	public static String MainUiActivity = "fm.xiami.yunos/fm.xiami.bmamba.activity.MainUiActivity";
	public static String appstoreActivity = "com.aliyun.wireless.vos.appstore/com.aliyun.wireless.vos.appstore.SlideActivity";
	public static String DialActivity = "com.yunos.alicontacts/com.yunos.alicontacts.activities.DialtactsContactsActivity";
	public static String PeopleActivity = "com.yunos.alicontacts/com.yunos.alicontacts.activities.PeopleActivity2";
	public static String mmsActivity = "com.android.mms/com.android.mms.ui.ConversationList";
	public static String mmsComposeActivity = "com.android.mms/com.android.mms.ui.ComposeMessageActivity";
	public static String UCMobileActivity = "com.UCMobile.yunos/com.uc.browser.InnerUCMobile";
	public static String saoyisaoActivity = "com.yunos.camera/com.yunos.camera.CameraActivity";
	public static String ThemeMainActivity = "com.yunos.theme.thememanager/com.yunos.theme.thememanager.main.ThemeMainActivity";
	public static String NotesListActivity = "com.aliyun.note/com.aliyun.note.activity.NotesListActivity";
	public static String emailActivity = "com.aliyun.mobile.email/com.aliyun.mobile.email.activity.setup.AccountSetupBasics";
	public static String calendarActivity = "com.android.calendar/com.android.calendar.AllInOneActivity";
	public static String VideoActivity = "com.aliyun.video/com.aliyun.video.VideoCenterActivity";
	public static String SecurityCenterActivity = "com.aliyun.SecurityCenter/com.aliyun.SecurityCenter.ui.SecurityCenterActivity";
	public static String minimapActivity = "com.autonavi.minimap.custom/com.autonavi.minimap.Splashy";
	public static String SettingsActivity = "com.android.settings/com.android.settings.Settings";
	public static String FileManagerActivity = "com.aliyunos.filemanager/com.aliyunos.filemanager.FileManagerAppFrame";
	public static String DeskClockActivity = "com.android.deskclock/com.android.deskclock.DeskClock";
	public static String CalculatorActivity = "com.yunos.calculator/com.yunos.calculator.Calculator";
	public static String FMRadioActivity = "com.yunos.FMRadio/com.yunos.FMRadio.FMRadioActivity";
	public static String HomeActivity = "com.aliyun.homeshell/com.aliyun.homeshell.Launcher";
	public static String UCMobileHomeActivity = "com.UCMobile.yunos/com.uc.browser.InnerUCMobile";
	public static String emailListActivity = "com.aliyun.mobile.email/com.aliyun.mobile.email.activity.EmailActivity";
	public static String NotesEditorActivity = "com.aliyun.note/com.aliyun.note.activity.TextEditorActivity";
	public static String NotesVoiceActivity = "com.aliyun.note/com.aliyun.note.activity.VoicePlayerActivity";
	public static String appNormalActivity = "com.aliyun.wireless.vos.appstore/com.aliyun.wireless.vos.appstore.NormalActivity";
	public static String ThemeDetailActivity = "com.yunos.theme.thememanager/com.yunos.theme.thememanager.detailtheme.OnlineDetailActivity";


	/**
	 *初始化时将弹出的无响应和异常停止提示框取消
	 * 
	 * @param strMessage
	 * @param assertSuccess
	 */
	public void cancleDialog() {
		//规避运营商提示和其他提示
		if(Waiter.waitForText("确定",10)){
			UiBox.clickByText("确定");
		}
    	if(Waiter.waitForText("更新",10)){
    		UiBox.clickByText("稍后再说");
    	}
	}
	
	/**
	 *初始化时将弹出的无响应和异常停止提示框取消
	 * 
	 * @param strMessage
	 * @param assertSuccess
	 */
	public void cancleFailDialog() {

		String FCActivity = "android/com.android.server.am.AppErrorDialog";
		if(ScreenBox.getLastWindowName().equals(FCActivity)){
			UiBox.clickByText("确定");
		}else if(new UiSelector().textContains("无响应") != null){
			UiBox.clickByText("确定");
		}	
	}
	
	/**
	 * 连续按几次返回键
	 * @param times
	 * @return
	 */
	public boolean goback(int times){
		try{
		for (int i =0 ;i<times; i++){
			 Keys.pressBack();		
		}
		 return true;
		}catch(Exception e){
 			e.printStackTrace();
 			return false;
		}
	}
	
	
	/**
	 *index传入屏幕期望显示的值，0是第一屏 ，1是第二屏，2是第三屏
	 * @return
	 */ 
	public boolean swipeScreen(int index) {
		int[][] Num = { { 248,802,255,255,255 },
				{ 270,803,255,255,255 }, { 290,803,255,255,255 } };

		int times = 0;
		while (times <= 3) {
			times++;
			Keys.pressHome();
			Keys.pressHome();
			if (ResponseTimeBox.clickAndWaitForPix(0, 0, Num[0][0], Num[0][1], Num[0][2],
					Num[0][3], Num[0][4], 2) < 0) {
				SysLog.error("手机未成功返回至桌面");
				continue;
			} else {
				for (int i = 0; i < index; i++) {
					ScreenBox.swipeLeft();
				}
				if (ResponseTimeBox.clickAndWaitForPix(0, 0, Num[index][0],
						Num[index][1], Num[index][2], Num[index][3],
						Num[index][4], 2) > 0) {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * 获取控件的X,Y坐标
	 * @param text
	 * @return 获取失败,返回-1
	 */
	public int[] getXYByText(String text){
		int[] xy = {-1, -1};
		int appXaxis = -1;
		int appYaxis = -1;
		try{
			UiObject obj = UiBox.getUiObject(new UiSelector().text(text));
			Assert.assertTrue("获取控件成功", obj != null);
			appXaxis = obj.getCenterX();
			appYaxis = obj.getCenterY(); 
		}catch(Exception e){
			e.printStackTrace();
		}
		xy[0] = appXaxis;
		xy[1] = appYaxis;
		return xy;
	}
	
	
	
	/**
	 * 点击 获取预期pixel的时间
	 * @param text
	 * @return
	 */
	public long getTimeclickAndWaitForPix(String text, String expectedActivity){
		int[] xy = getXYByText(text);
		if(xy[0] == -1 || xy[1] == -1){
			SysLog.info(">>>异常:获取控件失败");
			goback(3);
			return -2;
		}
		long time = clickAndWaitForScreenChangedandHoldtime(xy[0], xy[1], 2, 10); 
		Assert.assertEquals("进入预期界面", expectedActivity, ScreenBox.getCurrentWindowName());
		Sleep.sleepSeconds(1); 
		goback(3);
		return time;	
	}
	
	/**
	 * 点击 获取预期pixel的时间
	 * @return
	 */
	public long getTimeclickAndWaitForPix(int clickX, int clickY){
		long time = clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10); 
		Sleep.sleepSeconds(2);
		goback(4);
		return time;	
	}
	

	/**
	 * 打开应用后, 点击home, 获取点击home到桌面完全展示的时间
	 * @param Activity
	 * @return
	 */
	public long getTimeAfterClickHomeFormActivity(String Activity, String expectedActivity){
		Sleep.sleepSeconds(2); 
		Assert.assertTrue("成功打开"+Activity, UiBox.clickByTextAndWaitNewWindow(Activity));
		Sleep.sleepSeconds(2); 
		Assert.assertEquals("进入预期界面", expectedActivity, ScreenBox.getCurrentWindowName());
		Sleep.sleepSeconds(1); 
      if(	SystemInfoBox.getYunosVersion().contains("2.9")){
    		return clickAndWaitForBottomHalfScreenChanged(HomeX, HomeY);
		}
		else  {
			SysLog.info("使用3.0版本");
			return clickAndWaitForBottomHalfScreenChanged(HomeX1, HomeY1);
		}
		
	}
	
	
	
	/**
	 * 打开应用后, 点击home, 获取点击home到桌面完全展示的时间
	 * @param Activity
	 * @return
	 */
	public long getTimeAfterClickBackFormActivity(String Activity, String expectedActivity){
		Sleep.sleepSeconds(2); 
		Assert.assertTrue("成功打开"+Activity, UiBox.clickByTextAndWaitNewWindow(Activity));
		Sleep.sleepSeconds(2); 
		Assert.assertEquals("进入预期界面", expectedActivity, ScreenBox.getCurrentWindowName());
		Sleep.sleepSeconds(1); 

		if (SystemInfoBox.getYunosVersion().contains("2.9")) {
			return clickAndWaitForBottomHalfScreenChanged(BackX, BackY);
		} else {
			SysLog.info("使用3.0版本");
			return clickAndWaitForBottomHalfScreenChanged(BackX1, BackY1);
		}

	}
	
	/**
	 * 杀进程后打开指定应用获取时间
	 * @param PackageName
	 * @param Activity
	 * @return
	 */
	public long getTimeclickAndWaitForPixAfterKill(String PackageName,String Activity, String expectedActivity){
		Sleep.sleepSeconds(1); 
		Assert.assertTrue("初始:杀掉进程", PackageBox.forceStopPackage(PackageName));
		Sleep.sleepSeconds(1);
		return getTimeclickAndWaitForPix(Activity, expectedActivity);
	}
	
	
	/**
	 * 传入click的点坐标，通过全屏取点的方式自动获取响应时间
	 * @return
	 */
	public long clickAndWaitForScreenChanged(int clickX,int clickY){
		int StartX = 0;
		int StartY =ScreenBox.getStatusBarHeight();
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		return ResponseTimeBox.clickAndWaitForScreenChanged(clickX, clickY, StartX, StartY, EndX, EndY, 10);
	}
	
	/**
	 * 传入click的点坐标，通过全屏取点的方式自动获取响应时间
	 * @return
	 */
	public long longPressAndWaitForBottomHalfScreenChanged(String text){
		int StartX = 0;
		int StartY =ScreenBox.getScreenHeight()/2;
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		return ResponseTimeBox.longpressByTextAndWaitForScreenChanged(text, StartX, StartY, EndX, EndY, 10, 2);
	}
	
	/**
	 * 传入click的点坐标，以及hold时间和timeout时间，通过全屏取点的方式自动获取响应时间
	 * 适用于多数据的冷启动以及界面很长时间才出现变化的场景
	 * @return
	 */
	public long clickAndWaitForScreenChangedandHoldtime(int clickX,int clickY,int holdtime,int timeout){
		int StartX = 0;
		int StartY =ScreenBox.getStatusBarHeight();
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		return ResponseTimeBox.clickAndWaitForScreenChanged(clickX, clickY, StartX, StartY, EndX, EndY, holdtime,timeout);
	}
	
	/**
	 * 传入Drag的起始坐标和结束坐标，以及hold时间和timeout时间，通过全屏取点的方式自动获取响应时间
     *	适用于多数据的冷启动以及界面很长时间才出现变化的场景
	 * @return
	 */
	public long dragAndWaitForScreenChangedandHold(int dragX,int dragY,int dragEndX,int dragEndY,int holdtime,int timeout){
		int StartX = 0;
		int StartY =ScreenBox.getStatusBarHeight();
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		long time = ResponseTimeBox.dragAndWaitForPixScreenChanged(dragX, dragY,dragEndX, dragEndY,StartX, StartY, EndX, EndY, holdtime,timeout);
		return time;
	}
	
	/**
	 *  传入Drag的起始坐标和结束坐标，通过全屏取点的方式自动获取响应时间
	 * @return
	 */
	public long dragAndWaitForScreenChanged(int dragX,int dragY,int dragEndX,int dragEndY){
		int StartX = 0;
		int StartY =ScreenBox.getStatusBarHeight();
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		long time = ResponseTimeBox.dragAndWaitForPixScreenChanged(dragX, dragY,dragEndX, dragEndY,StartX, StartY, EndX, EndY,10);
		return time;
	}
	
	/**
	 *  等待靠上部分屏幕变化 屏幕高度2/3
	 *  适用于Toast的场景
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public long clickAndWaitForTopHalfScreenChanged(int clickX,int clickY){
		int StartX = 0;
		int StartY =ScreenBox.getStatusBarHeight();
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-ScreenBox.getScreenHeight()/3;
		return ResponseTimeBox.clickAndWaitForScreenChanged(clickX, clickY, StartX, StartY, EndX, EndY, 2, 10);
	}
	
	/**
	 *  等待下半部分屏幕变化
	 *  适用于Toast的场景
	 * @return
	 */
	public long clickAndWaitForBottomHalfScreenChanged(int clickX,int clickY){
		int StartX = 0;
		int StartY =ScreenBox.getScreenHeight()/2;
		int EndX =ScreenBox.getScreenWidth()-1;
		int EndY =ScreenBox.getScreenHeight()-1;
		return ResponseTimeBox.clickAndWaitForScreenChanged(clickX, clickY, StartX, StartY, EndX, EndY, 2, 10);
	}
//	/**
//	 * 等待下半部分屏幕变化
//	 *  适用于Toast的场景
//	 * @param Activity
//	 * @return
//	 */
//	public long clickAndWaitForBottomHalfScreenChanged1(String Activity){
//		Assert.assertTrue("成功打开"+Activity, UiBox.clickByTextAndWaitNewWindow(Activity));
//		Sleep.sleepSeconds(2); 
//		int StartX = 0;
//		int StartY =ScreenBox.getScreenHeight()/2;
//		int EndX =ScreenBox.getScreenWidth()-1;
//		int EndY =ScreenBox.getScreenHeight()-1;
//		return UiBox.clickAndWaitForScreenChanged(HomeX, HomeY, StartX, StartY, EndX, EndY, 2,10);
//	}	 
//	/**
//	 * 等待下半部分屏幕变化
//	 *  适用于Toast的场景
//	 * @param Activity
//	 * @return
//	 */
//	public long clickAndWaitForBottomHalfScreenChanged2(String Activity){
//		Assert.assertTrue("成功打开"+Activity, UiBox.clickByTextAndWaitNewWindow(Activity));
//		Sleep.sleepSeconds(2); 
//		int StartX = 0;
//		int StartY =ScreenBox.getScreenHeight()/2;
//		int EndX =ScreenBox.getScreenWidth()-1;
//		int EndY =ScreenBox.getScreenHeight()-1;
//		return UiBox.clickAndWaitForScreenChanged(BackX, BackY, StartX, StartY, EndX, EndY, 2, 10);
//	}	 
}
	
	

