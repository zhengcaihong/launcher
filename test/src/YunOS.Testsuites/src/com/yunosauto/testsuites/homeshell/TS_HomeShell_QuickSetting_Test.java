package com.yunosauto.testsuites.homeshell;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yaf.framework.Assert;
import com.yaf.framework.BaseTest;
import com.yaf.framework.context.HomeShellContext;
import com.yaf.framework.core.Device;
import com.yaf.framework.core.Sleep;
import com.yaf.framework.core.channel.WifiSocketChannel;
import com.yaf.framework.env.ConfigParser;
import com.yaf.framework.interfaces.DeviceFace;
import com.yaf.framework.interfaces.vo.UiObject;
import com.yaf.framework.interfaces.vo.UiSelector;
import com.yaf.framework.interfaces.wrapper.AccountAPIBox;
import com.yaf.framework.interfaces.wrapper.AccountBox;
import com.yaf.framework.interfaces.wrapper.DeviceBox;
import com.yaf.framework.interfaces.wrapper.Keys;
import com.yaf.framework.interfaces.wrapper.NetWorkBox;
import com.yaf.framework.interfaces.wrapper.PackageBox;
import com.yaf.framework.interfaces.wrapper.PixelPerfBox;
import com.yaf.framework.interfaces.wrapper.ScreenBox;
import com.yaf.framework.interfaces.wrapper.UiBox;
import com.yaf.framework.interfaces.wrapper.Waiter;

public class TS_HomeShell_QuickSetting_Test extends BaseTest {
	Utils utils = new Utils();
	CommonOperation co = new CommonOperation();

	private String model = Utils.Model.hotRun.getName();
	private String wifissid = ConfigParser.getProperty("wifiOfWpaCompanySsid1");
	private String wifiidentify = ConfigParser.getProperty("wifiOfWpaCompanyName1");
	private String wifipassword = ConfigParser.getProperty("wifiOfWpaCompanyPass1");

	@Before
	public void setUp() {
		Sleep.sleepSeconds(2);
		Assert.assertTrue("数据初始化:解锁屏幕", ScreenBox.unLock());
		Assert.assertTrue("初始测试环境: 连续点击back键(保证在桌面状态)", co.goback(5));
		Sleep.sleepSeconds(2);
	}

	@After
	public void tearDown() {
		Sleep.sleepSeconds(2);
		Assert.assertTrue("清场测试环境: 连续点击back键(保证在桌面状态)", co.goback(5));
		if (!NetWorkBox.checkHttpClient()) {

			Assert.assertTrue("openWifi", NetWorkBox.openWifi());
			Sleep.sleepSeconds(3);
//			if ("".equalsIgnoreCase(wifiidentify)) {
//				wifiidentify = null;
//			}
			Assert.assertTrue("connectWifi", NetWorkBox.connectWifi(wifissid,
					wifipassword, wifiidentify));
		}
	}

	private boolean clearWifi() {
		try {
			if (NetWorkBox.isWifiOpened()) {
				Assert.assertTrue("关闭wifi", NetWorkBox.closeWifi());
				Sleep.sleepSeconds(3);
				Assert.assertTrue("清除wifi历史，避免自动连接wifi",
						NetWorkBox.clearWifiHistory());
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		return true;
	}


	/** @author xueya.hb
	 * TC_testQuickStart_001
	 * CaseID: 5649162
	 * Module: 通知中心
	 * Title: 查看快捷开关中的设置功能
	 * 优先级：P1:
	 * 前置条件：
	 * 步骤详情：
	 * 步骤1：下拉状态栏;点击“快捷开关”Tab;查看设置Icon；
	 * 预期：设置Icon正常显示；
	 * 步骤2：点击设置Icon；
	 * 预期：设置页面打开
	 * 步骤3：点击返回键
	 * 预期：返回桌面
	 * 步骤4：重复1-2，按home键
	 * 预期：返回桌面；
	 */
    @Test	
	public void TC_testQuickStart_001() {
		String casename = Thread.currentThread().getStackTrace()[1]
				.getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub

			}

			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				System.out.println("starty:" + Constants.STATUSBAR_START_Y);
				System.out.println("startx:" + Constants.STATUSBAR_END_X);
				System.out.println("endy:" + Constants.STATUSBAR_END_Y);
				Assert.assertTrue("步骤1.1：下拉状态栏", UiBox.drag(
						Constants.STATUSBAR_START_X,
						Constants.STATUSBAR_START_Y, Constants.STATUSBAR_END_X,
						Constants.STATUSBAR_END_Y));

				Sleep.sleepSeconds(2);
				Assert.assertTrue("步骤1.2：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("预期结果：设置Icon正常显示",
						UiBox.getUiObject(new UiSelector().id("setting"))
								.isEnabled());

				UiObject settingButton = UiBox.getUiObject(new UiSelector()
						.id("setting"));
				int clickX = settingButton.getCenterX();
				int clickY = settingButton.getCenterY();
				Assert.assertTrue("点击设置按钮", true);
				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(
						clickX, clickY, 2, 10);
				System.out.println("reposeTime:" + reposeTime);
				Assert.assertTrue("预期结果：设置界面打开",
						Waiter.waitForPage(Constants.SETTINGS_ACTIVITY));
				Assert.assertTrue("步骤3：点击返回键", Keys.pressBack());
				Assert.assertTrue("预期结果：返回桌面",
						Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
				Sleep.sleepSeconds(2);
				return reposeTime;
			}

			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub

			}
		};

		testCaseModel.testCaseHot(model, casename);

	}

	/** @author xueya.hb
	 * TC_testQuickStart_002
	 * CaseID: 5649172
	 * Module: 通知中心
	 * Title: 快捷设置关闭和开启wlan，查看设置
	 * 优先级：P1:
	 * 前置条件：
	 * 手机wlan为开启且wlan开关显示在快捷开关中
	 * 步骤详情：
	 * 步骤1：在通知中心快捷开关界面关闭wlan，查看设置中心wlan状态；
	 * 预期：设置中心的wlan关闭；
	 * 步骤2：在通知中心快捷开关界面开启wlan，查看设置中心wlan状态；
	 * 预期：设置中心的wlan开启 
	 *
	 */
    @Test
	public void TC_testQuickStart_002() {
		Assert.assertTrue("数据初始化:关闭wlan,清除wifi历史", clearWifi());

		Assert.assertTrue("预置条件:waln开启", NetWorkBox.openWifi());

		Assert.assertTrue("步骤1.1：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.2：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.3：在通知中心快捷开关界面关闭wlan", UiBox.clickByText("WLAN"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("预期结果：wlan关闭", !NetWorkBox.isWifiOpened());
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤2：在通知中心快捷开关界面开启wlan", UiBox.clickByText("WLAN"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("预期结果：wlan打开", NetWorkBox.isWifiOpened());
	}

	/** @author xueya.hb
	 * TC_testQuickStart_003
	 * CaseID: 5649173
	 * Module: 通知中心
	 * Title: 快捷设置关闭和开启移动网络，查看设置
	 * 优先级：P1:
	 * 前置条件：
	 * 手机移动网络为开启且移动网络开关显示在快捷开关中
	 * 步骤详情：
	 * 步骤1：在通知中心快捷开关界面关闭移动网络，查看设置中心移动网络状态
	 * 预期：设置中心的移动网络关闭	
	 * 步骤2：在通知中心快捷开关界面开启移动网络，查看设置中心移动网络状态
	 * 预期：设置中心的移动网络开启 
	 *
	 */
    @Test
	public void TC_testQuickStart_003() {

		if (!NetWorkBox.isMobileDataOpened()) {
			Assert.assertTrue("预置条件:移动网络开启", NetWorkBox.openMobileData());
		}

		Assert.assertTrue("步骤1.1：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.2：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.3：在通知中心快捷开关界面关闭移动网络", UiBox.clickByText("移动上网"));
		Sleep.sleepSeconds(3);

		Assert.assertTrue("预期结果：移动网络关闭", !NetWorkBox.isMobileDataOpened());
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤2：在通知中心快捷开关界面开启移动网络", UiBox.clickByText("移动上网"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("预期结果：移动网络开启", NetWorkBox.isMobileDataOpened());
	}

	/** @author xueya.hb
	 * TC_testQuickStart_004
	 * CaseID: 5649173
	 * Module: 通知中心
	 * Title: 快捷设置开启震动模式，查看设置
	 * 优先级：P1:
	 * 前置条件：
	 * 手机声音模式为非振动模式且声音开关显示在快捷开关中
	 * 步骤详情：
	 * 步骤 1：在通知中心快捷开关界面开启震动模式，查看设置中心声音模式
	 * 预期：设置中心显示为震动模式（仅会议）	
	 * 
	 *
	 */
    @Test
	public void TC_testQuickStart_004() {

		Assert.assertTrue("数据初始化：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("数据初始化：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);

		// 先判断当前是什么模式，如果是振动模式，则设置为响铃模式，如果是非振动模式，直接设置为振动模式

		if (UiBox.getUiObject(new UiSelector().text(Constants.VIBRATE)) != null) {

			Assert.assertTrue("预置条件:声音模式静音模式",
					UiBox.getUiObject(new UiSelector().text(Constants.VIBRATE))
							.click(0.5f, 0.5f));
			Assert.assertTrue("步骤1.01：从静音模式设置为响铃模式",
					UiBox.clickByText(Constants.SILENT));
			Sleep.sleepSeconds(2);
			Assert.assertTrue("步骤1.02：从响铃模式设置为振动模式",
					UiBox.clickByText(Constants.SOUND));

		} else if (UiBox.getUiObject(new UiSelector().text(Constants.SOUND)) != null) {
			Assert.assertTrue("步骤1.1：从响铃模式设置为振动模式",
					UiBox.clickByText(Constants.SOUND));
		} else {
			Assert.assertTrue("步骤1.01：从静音模式设置为响铃模式",
					UiBox.clickByText(Constants.SILENT));
			Sleep.sleepSeconds(2);
			Assert.assertTrue("步骤1.02：从响铃模式设置为振动模式",
					UiBox.clickByText(Constants.SOUND));
		}

		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.2：按返回键，返回桌面", Keys.pressBack());
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.3：打开设置界面",
				PackageBox.launchActivity(Constants.SETTINGS_ACTIVITY));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("步骤1.4：进入到设置声音", UiBox.clickByText("声音"));

		Assert.assertTrue(
				"预期结果：设置中心显示为振动模式（仅会议）",
				UiBox.getUiObject(new UiSelector().text(Constants.MEETING_MODE)) != null);

		Sleep.sleepSeconds(1);
		Assert.assertTrue("点击返回键，返回到桌面", co.goback(4));
		Assert.assertTrue("点击home键", Keys.pressHome());
	}

	/** @author xueya.hb
	 * TC_testQuickStart_005
	 * CaseID: 5649209
	 * Module: 通知中心
	 * Title: 快捷开关锁屏
	 * 优先级：P1:
	 * 前置条件：
	 * 锁屏图标在快捷开关设置中（可进入快捷开关更多界面调整位置）
	 * 步骤详情：
	 * 步骤 1：下拉通知栏进入快捷开关，点击锁屏按钮
	 * 预期：手机锁屏
	 * 步骤2：解锁手机
	 * 预期：进入到进入通知中心前所在界面
	 *
	 */
    @Test
	public void TC_testQuickStart_005() {

		Assert.assertTrue("步骤1.1：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.2：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.3：点击\"更多\"按钮", UiBox.clickByText("更多"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.4：向上滑屏", ScreenBox.swipeUp());
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.5：点击锁屏", UiBox.clickByText("锁屏"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果：手机锁屏", ScreenBox.isScreenLocked());
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤2：手机解锁", ScreenBox.unLock());
		Assert.assertTrue("预期结果：进入到通知中心前所在界面",
				Waiter.waitForPage(Constants.QUICK_SETTING_ACTIVITY));

		Sleep.sleepSeconds(1);
		Assert.assertTrue("点击返回键，返回到桌面", co.goback(4));
		Assert.assertTrue("点击home键", Keys.pressHome());
	}

	// /** @author xueya.hb
	// * TC_testQuickStart_006
	// * CaseID: 5649209
	// * Module: 通知中心
	// * Title: 快捷开关锁屏
	// * 优先级：P1:
	// * 前置条件：
	// * 手机定位为开
	// * 步骤详情：
	// * 步骤 1：从设置中心关闭定位，查看通知中心快捷开关定位开关
	// * 预期：通知中心快捷开关的定位为关
	// * 步骤2：从设置中心开启定位，查看通知中心快捷开关定位开关
	// * 预期：通知中心快捷开关的定位为开，状态栏不显示图标
	// *
	// */
    // @Test
	// public void TC_testQuickStart_006() {
	//
	//
	//
	// Assert.assertTrue("预置条件:打开手机定位", NetWorkBox.openGPSLocation());
	//
	// Assert.assertTrue("步骤1.1：打开设置界面",
	// PackageBox.launchActivity(Constants.SETTINGS_ACTIVITY));
	// Sleep.sleepSeconds(3);
	// Assert.assertTrue("步骤1.2：向上划屏",ScreenBox.swipeUp());
	// Sleep.sleepSeconds(1);
	// Assert.assertTrue("步骤1.3：进入到定位设置界面", UiBox.clickByText(Constants.GPS));
	// Sleep.sleepSeconds(1);
	// Assert.assertTrue("步骤1.4：点击定位",UiBox.clickByText("定位"));
	//
	// Assert.assertTrue("步骤1.5：下拉状态栏", UiBox.drag(
	// Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
	// Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
	// Sleep.sleepSeconds(2);
	// Assert.assertTrue("步骤1.6：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
	// Sleep.sleepSeconds(1);
	// // Assert.assertTrue("预期结果:通知中心快捷开关的定位为关",UiBox.);
	//
	// }
	/** @author xueya.hb
	 * TC_testQuickStart_007
	 * CaseID: 5649163
	 * Module: 通知中心
	 * Title: 在快捷开关Tab页修改一些设置后点击该页的设置Icon进入设置页面查看修改的设置
	 * 优先级：P1:
	 * 前置条件：
	 * 手机wifi为开启，蓝牙关闭
	 * 步骤详情：
	 * 步骤1：下拉状态栏;点击“快捷开关”Tab;关闭WIFI，打开蓝牙；
	 * 预期：WIFI被关闭，蓝牙被打开；
	 * 步骤2：点击“快捷开关”Tab的设置Icon；
	 * 预期：设置中心的wlan开启 
	 * 步骤3: 在设置中检查WLAN和蓝牙的状态；
	 * 预期：WIFI和蓝牙状态与快捷开关Tab中显示一致；
	 */
    @Test
	public void TC_testQuickStart_007() {
		Assert.assertTrue("数据初始化:关闭wlan,清除wifi历史", clearWifi());

		Assert.assertTrue("预置条件:waln开启", NetWorkBox.openWifi());
		Assert.assertTrue("预置条件：蓝牙打开", NetWorkBox.closeBluetooth());
		Sleep.sleepSeconds(3);

		Assert.assertTrue("步骤1.1：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.2：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.3：在通知中心快捷开关界面关闭wifi", UiBox.clickByText("WLAN"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("预期结果：wlan关闭", !NetWorkBox.isWifiOpened());
		Sleep.sleepSeconds(1);
		Assert.assertTrue("步骤1.4：在通知中心快捷开关界面打开蓝牙", UiBox.clickByText("蓝牙"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("预期结果：蓝牙打开", NetWorkBox.isBluetoothOpened());
		Assert.assertTrue(
				"步骤2：在通知中心快捷开关界面点击设置图标",
				UiBox.getUiObject(new UiSelector().id("setting")).click(0.5f,
						0.5f));
		Assert.assertTrue("预期结果：设置界面打开",
				Waiter.waitForPage(Constants.SETTINGS_ACTIVITY));
		Assert.assertTrue("步骤3.1：点击wlan，进入wlan设置界面", UiBox.clickByText("WLAN"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue(
				"预期结果：wlan显示关闭",
				!UiBox.getUiObject(new UiSelector().id("switchWidget"))
						.isChecked()
						&& UiBox.getUiObject(new UiSelector().text("WLAN")) != null);
		Assert.assertTrue("步骤3.2：点击返回键，返回上一级设置界面", Keys.pressBack());
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤3.3：点击蓝牙，进入蓝牙设置界面", UiBox.clickByText("蓝牙"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue(
				"预期结果：蓝牙显示关闭",
				UiBox.getUiObject(new UiSelector().id("switchWidget"))
						.isChecked()
						&& UiBox.getUiObject(new UiSelector().text("蓝牙"))
								.isEnabled());

	}

	/** @author xueya.hb
	 * TC_testQuickStart_008
	 * CaseID: 5649188
	 * Module: 通知中心
	 * Title: 设置开启震动声音模式，查看通知中心快捷开关
	 * 优先级：P1:
	 * 前置条件：
	 * 手机当前非振动声音模式
	 * 步骤详情：
	 * 步骤1：从设置中心开启震动声音模式，查看通知中心快捷开关声音模式
	 * 预期：通知中心快捷开关声音显示为震动声音模式
	 * 
	 */
    @Test
	public void TC_testQuickStart_008() {
		Assert.assertTrue("数据初始化:进入设置界面",
				PackageBox.launchActivity(Constants.SETTINGS_ACTIVITY));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("数据初始化：点击声音，进入声音设置界面", UiBox.clickByText("声音"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("数据初始化：点击情景模式，进入情景模式设置界面", UiBox.clickByText("情景模式"));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预置条件：设置情景模式为标准", UiBox.clickByText("标准"));
		Sleep.sleepSeconds(3);
		Assert.assertTrue("步骤1.1：设置情景模式为振动声音模式", UiBox.clickByText("仅振动"));
		Sleep.sleepSeconds(2);

		Assert.assertTrue("步骤1.2：下拉状态栏", UiBox.drag(
				Constants.STATUSBAR_START_X, Constants.STATUSBAR_START_Y,
				Constants.STATUSBAR_END_X, Constants.STATUSBAR_END_Y));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("步骤1.3：点击\"快捷开关\"", UiBox.clickByText("快捷开关"));
		Sleep.sleepSeconds(1);

		Assert.assertTrue("预期结果：通知中心快捷开关声音显示为震动声音模式",
				UiBox.getUiObject(new UiSelector().text("振动")).isEnabled());

	}

}
