package com.yunosauto.testsuites.homeshell;

import java.io.File;
import java.util.List;

import javax.management.loading.PrivateClassLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

import com.aliyun.framework.log.SysLog;
import com.yaf.framework.Assert;
import com.yaf.framework.BaseTest;
import com.yaf.framework.core.Sleep;
import com.yaf.framework.env.RunEnvManager;
import com.yaf.framework.interfaces.wrapper.Checker;
import com.yaf.framework.interfaces.wrapper.FilesBox;
import com.yaf.framework.interfaces.wrapper.HomeShellBox;
import com.yaf.framework.interfaces.wrapper.Keys;
import com.yaf.framework.interfaces.wrapper.ScreenBox;
import com.yaf.framework.interfaces.wrapper.SdCardBox;
import com.yaf.framework.interfaces.wrapper.UiBox;
import com.yaf.framework.interfaces.wrapper.Waiter;
import com.yaf.framework.interfaces.wrapper.api.HomeShellAPIBox;

public class TS_HomeShell_InterfaceTest extends BaseTest {

	private String TAG = "TS_HomeShell_InterfaceTest";
	private CommonOperation co = new CommonOperation();

	@Before
	public void setUp() throws Exception {
		Assert.assertTrue("初始测试环境:清除所有的通知", Checker.clearStoredNotifications());
		Assert.assertTrue("初始测试环境:连续点击back键(保证在桌面状态)", co.goback(5));
	}

	@After
	public void tearDown() throws Exception {
		Assert.assertTrue("清理测试环境:连续点击back键(保证在桌面状态)", co.goback(5));
		Assert.assertTrue("清理测试环境:点击home键(保证在桌面状态)", Keys.pressHome());
	}

	/**
	 * TestCase Description
	 * 
	 * @name 添加简单自定义快捷方式，验证基本添加和点击功能
	 * @text_step 1、添加快捷方式“Test001”到桌面 2、检测是否创建成功 3、点击快捷方式打开拨号界面
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test001() {
		String name = "Test001";
		// 可创建成功
		String action = "com.aliyun.homeshell.action.INSTALL_SHORTCUT";
		String intentAction = "android.intent.action.DIAL";

		if (HomeShellBox.isShortcutExist(name)) {
			Assert.assertTrue("清理环境：卸载快捷方式\"Test001\"", true);
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1：添加快捷方式\"Test001\"");
		HomeShellAPIBox
				.test_InstallShortcutReceiver(action, name, intentAction);
		// Assert.assertTrue("预期结果1：添加快捷方式\"Test001\"成功",
		// Checker.isToastExist(name + " 已成功添加至桌面"));
		Sleep.sleepSeconds(2);

		boolean bExist = checkShortCutIsExist("1.1", name);

		Assert.assertTrue("预期结果1.1：图标创建成功", bExist);
		Assert.assertTrue("步骤2：点击快捷方式\"Test001\"，启动应用",
				UiBox.clickByTextAndWaitNewWindow(name));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果2：进入到拨号盘界面正常",
				Waiter.waitForPage(co.DialActivity));
		Sleep.sleepSeconds(2);
		Keys.pressBack();
		Sleep.sleepSeconds(2);
		SysLog.info("步骤3：卸载快捷方式\"Test001\"");
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果3：卸载快捷方式\"Test001\"成功",
				!HomeShellBox.isShortcutExist(name));
		Keys.pressBack();
	}

	/**
	 * TestCase Description
	 * 
	 * @name 删除简单自定义快捷方式，验证基本删除功能
	 * @text_step 1、删除快捷方式“Test001”到桌面 2、检测是否创建成功 3、点击快捷方式打开拨号界面
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type UninstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@SuppressWarnings("deprecation")
	@Test
	@Ignore
	public void TC_test002() {
		String name = "Test001";
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		if (HomeShellBox.isShortcutExist(name)) {
			junit.framework.Assert.fail(TAG
					+ ": UnInstallShortcut Test001 is fail.");
		}
	}

	/**
	 * TestCase Description
	 * 
	 * @name 模拟intent插入脏数据
	 * @text_step 1、添加快捷方式“Test003”到桌面 2、检测是否创建成功 3、点击快捷方式提示：“该应用无法使用”
	 *            4、卸载Test003
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test003() {
		String name = "Test003";
		String action = "com.aliyun.homeshell.action.INSTALL_SHORTCUT";
		String intentAction = "android.intent.action.222";
		if (HomeShellBox.isShortcutExist(name)) {
			SysLog.info("清理环境：卸载快捷方式\"Test003\"");
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1：添加快捷方式\"Test003\"到桌面，快捷图标action不存在");
		HomeShellAPIBox
				.test_InstallShortcutReceiver(action, name, intentAction);
		// Assert.assertTrue("预期结果1：添加快捷方式成功",
		// Checker.isToastExist(name + " 已成功添加至桌面"));
		Sleep.sleepSeconds(2);

		boolean bExist = checkShortCutIsExist("1.1", name);

		Assert.assertTrue("预期结果1.1：图标创建成功", bExist);

		Assert.assertTrue("步骤2.2：点击快捷方式\"Test003\"，启动应用",
				UiBox.clickByText(name));
		Assert.assertTrue("预期结果2：提示应用无法启动", Checker.isToastExist("该应用无法使用"));
		Sleep.sleepSeconds(2);

		SysLog.info("步骤3：卸载快捷方式\"Test003\"");
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果3：卸载快捷方式\"Test003\"成功",
				!HomeShellBox.isShortcutExist(name));
		Keys.pressBack();
		// HomeShellAPIBox.test_InstallShortcutReceiver(action, name,
		// intentAction);
		// Assert.assertTrue("添加成功",Checker.isToastExist(name+" 已成功添加至桌面"));
		// Sleep.sleepMillisecond(3000);
		// // Assert.assertTrue("图标创建成功", Waiter.waitForText(name+" 已成功添加至桌面"));
		// UiBox.clickByText(name);
		// Assert.assertTrue("提示无法使用",Checker.isToastExist("该应用无法使用"));
		// HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		// Keys.pressHome();
	}

	/**
	 * TestCase Description
	 * 
	 * @name 添加简单自定义快捷方式，验证自定义图片正常情况下安装是否成功
	 * @text_step 1、添加快捷方式“Test004”到桌面，自定义图片存在 2、检测是否创建成功 3、点击快捷方式打开拨号界面
	 *            4、卸载快捷方式
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test004() {
		String name = "Test004";
		if (HomeShellBox.isShortcutExist(name)) {
			SysLog.info("清理环境：卸载快捷方式");
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1：添加快捷方式\"Test004\"到桌面，自定义图片存在");
		HomeShellAPIBox.setInstallShortcutIcon("a.png");
		HomeShellAPIBox
				.setInstallAction(HomeShellAPIBox.ACTION_INSTALL_SHORTCUT);
		HomeShellAPIBox
				.setInstallShortcutIntentAction(HomeShellAPIBox.ACTION_DIAL);
		HomeShellAPIBox.setInstallShortcutName(name);
		HomeShellAPIBox.sendInstallShortcutIntent();
		// Assert.assertTrue("步骤1：添加快捷方式\"Test004\"成功",
		// Checker.isToastExist(name + " 已成功添加至桌面"));

		Sleep.sleepSeconds(2);

		boolean bExist = checkShortCutIsExist("1.1", name);

		Assert.assertTrue("预期结果1.1：图标创建成功", bExist);
		Assert.assertTrue("步骤2.2：点击快捷方式\"Test004\"，启动应用",
				UiBox.clickByTextAndWaitNewWindow(name));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果2：进入到拨号盘界面正常",
				Waiter.waitForPage(co.DialActivity));
		Sleep.sleepSeconds(2);
		SysLog.info("步骤3：卸载快捷方式\"Test004\"");
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果3：卸载快捷方式\"Test004\"成功",
				!HomeShellBox.isShortcutExist(name));
		Keys.pressBack();
		// HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		// Keys.pressBack();
		// Keys.pressHome();
	}

	/**
	 * TestCase Description
	 * 
	 * @name 添加简单自定义快捷方式，验证自定义图片非正常情况下(b.png不存在)安装是否成功
	 * @text_step 1、添加快捷方式“Test004”到桌面，自定义图片不存在 2、检测是否创建成功 3、点击快捷方式打开拨号界面
	 *            4、卸载快捷方式
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test005() {
		String name = "Test005";
		if (HomeShellBox.isShortcutExist(name)) {
			Assert.assertTrue("清理环境：卸载快捷方式\"Test005\"", true);
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1：添加快捷方式\"Test005\"到桌面，自定义图片不存在");
		HomeShellAPIBox.setInstallShortcutIcon("b.png");
		HomeShellAPIBox
				.setInstallAction(HomeShellAPIBox.ACTION_INSTALL_SHORTCUT);
		HomeShellAPIBox
				.setInstallShortcutIntentAction(HomeShellAPIBox.ACTION_DIAL);
		HomeShellAPIBox.setInstallShortcutName(name);
		HomeShellAPIBox.sendInstallShortcutIntent();

		// Assert.assertTrue("预期结果1：添加快捷方式\"Test005\"成功",
		// Checker.isToastExist(name + " 已成功添加至桌面"));
		Sleep.sleepSeconds(2);

		boolean bExist = checkShortCutIsExist("1.1", name);

		Assert.assertTrue("预期结果1.1：快捷方式创建成功", bExist);
		Assert.assertTrue("步骤2.2：点击快捷方式\"Test005\"，启动应用",
				UiBox.clickByTextAndWaitNewWindow(name));
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果2：进入到拨号盘界面正常",
				Waiter.waitForPage(co.DialActivity));
		Sleep.sleepSeconds(2);
		SysLog.info("步骤3：卸载快捷方式\"Test005\"");
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		Sleep.sleepSeconds(2);
		Assert.assertTrue("预期结果3：卸载快捷方式\"Test005\"成功",
				!HomeShellBox.isShortcutExist(name));
		Keys.pressBack();
	}

	/**
	 * TestCase Description
	 * 
	 * @name 添加简单自定义快捷方式，验证自定义图片非正常情况下(c.png存在，但大小异常)安装是否成功
	 * @text_step 1、添加快捷方式“Test006”到桌面，自定义图片异常 2、检测是否创建成功
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test006() {
		String name = "Test006";
		if (HomeShellBox.isShortcutExist(name)) {
			Assert.assertTrue("清理环境：卸载快捷方式\"Test006\"", true);
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1.1：添加快捷方式\"Test006\",自定义图标异常");
		HomeShellAPIBox.setInstallShortcutIcon("c.png");
		HomeShellAPIBox
				.setInstallAction(HomeShellAPIBox.ACTION_INSTALL_SHORTCUT);
		HomeShellAPIBox.setInstallShortcutIntentAction("");
		HomeShellAPIBox.setInstallShortcutName(name);
		HomeShellAPIBox.sendInstallShortcutIntent();
		Sleep.sleepSeconds(2);

		Assert.assertTrue("预期结果1.1：快捷方式\"Test006\"不存在",
				!HomeShellBox.isShortcutExist(name));
		Sleep.sleepSeconds(2);

	}

	/**
	 * TestCase Description
	 * 
	 * @name 添加简单自定义快捷方式，验证自定义图片非正常情况下安装是否成功
	 * @text_step 1、添加快捷方式“Test007”到桌面，无图标，无intent内容 2、检测是否创建成功
	 *            3、点击快捷方式是否提示“该应用无法使用” 4、卸载快捷方式
	 * @time 2014.10.29
	 * @user zhangxuan
	 * @mode_type InstallShortcut
	 * @result_id
	 * @priority_id
	 */
	@Test
	public void TC_test007() {
		String name = "Test007";
		if (HomeShellBox.isShortcutExist(name)) {
			Assert.assertTrue("清理环境：卸载快捷方式\"Test007\"", true);
			HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		}
		SysLog.info("步骤1：添加快捷方式“Test007”到桌面，无图标，无intent内容");
		HomeShellAPIBox
				.setInstallAction(HomeShellAPIBox.ACTION_INSTALL_SHORTCUT);
		HomeShellAPIBox.setInstallShortcutIntentAction("");
		HomeShellAPIBox.setInstallShortcutName(name);
		HomeShellAPIBox.sendInstallShortcutIntent();

		Sleep.sleepSeconds(2);
		boolean bExist = checkShortCutIsExist("1.1", name);

		Assert.assertTrue("预期结果1：快捷方式创建成功", bExist);
		
		SysLog.info("步骤2：点击快捷方式“test007”");
		UiBox.clickByText(name);
		
		Assert.assertTrue("预期结果2：提示“test007”无法使用", Checker.isToastExist("该应用无法使用"));
		Sleep.sleepSeconds(2);
		HomeShellAPIBox.test_UnInstallShortcutReceiver(name);
		Sleep.sleepSeconds(2);
	}

	// /**TestCase Description
	// * @name 添加简单自定义快捷方式，验证自定义图片非正常情况下安装是否成功
	// * @text_step
	// * 1、添加快捷方式“Test008”到桌面，无图标，无intent内容
	// * 2、检测是否创建成功
	// * 3、点击快捷方式是否提示“该应用无法使用”
	// * 4、卸载快捷方式
	// * @time 2014.10.29
	// * @user zhangxuan
	// * @mode_type InstallShortcut
	// * @result_id
	// * @priority_id
	// */
	// @Test
	// public void test008() {
	// // String name = "Test008";
	// StringBuilder name = new StringBuilder();
	// for (int index = 0; index <= 500; index++) {
	// name.append("!@#$%^&*():;><,.ad");
	// // name += "!@#$%^&*():;><,.adfgad?dd1^8&*0313afdsf?||\\";
	// }
	// HomeShellAPIBox.setInstallAction(HomeShellAPIBox.ACTION_INSTALL_SHORTCUT);
	// HomeShellAPIBox.setInstallShortcutIntentAction("");
	// HomeShellAPIBox.setInstallShortcutName(name.toString());
	// HomeShellAPIBox.sendInstallShortcutIntent();
	// Assert.assertTrue("添加成功",Checker.isToastExist(name+" 已成功添加至桌面"));
	// Sleep.sleepMillisecond(3000);
	// UiBox.clickByText(name.toString());
	// Assert.assertTrue("提示无法使用",Checker.isToastExist("该应用无法使用"));
	// // HomeShellAPIBox.test_UnInstallShortcutReceiver(name.toString());
	// }

	/**
	 * 检测快捷方式是否在桌面上已经存在
	 *
	 * @param step
	 *            当前验证步骤
	 * @param name
	 *            快捷方式名称
	 * @return
	 */
	private boolean checkShortCutIsExist(String step, String name) {
		boolean bExist = false;
		for (int i = 0; i < 14; i++) {
			if (Checker.isExistByText(name)) {
				SysLog.info("步骤" + step + "：滑动屏幕到快捷方式\"" + name + "\"所在屏");
				bExist = true;
				break;
			}

			ScreenBox.swipeLeft();
			Sleep.sleepSeconds(2);
		}
		return bExist;
	}

	/**
	 * 进入到快捷方式所在的屏幕
	 * @param name 快捷方式的名字
	 */
	private void goToScreenByShortCut(String name) {
		for (int i = 0; i < 14; i++) {
			if (Checker.isExistByText(name)) {
				break;
			}

			ScreenBox.swipeLeft();
			Sleep.sleepSeconds(2);
		}
	}
}