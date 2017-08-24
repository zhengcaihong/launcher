package com.yunosauto.testsuites.homeshell;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.yaf.framework.Assert;
import com.yaf.framework.BaseTest;
import com.yaf.framework.core.Sleep;
import com.yaf.framework.env.ConfigParser;
import com.yaf.framework.interfaces.vo.UiObject;
import com.yaf.framework.interfaces.vo.UiSelector;
import com.yaf.framework.interfaces.wrapper.Keys;
import com.yaf.framework.interfaces.wrapper.NetWorkBox;
import com.yaf.framework.interfaces.wrapper.PackageBox;
import com.yaf.framework.interfaces.wrapper.ResponseTimeBox;
import com.yaf.framework.interfaces.wrapper.ScreenBox;
import com.yaf.framework.interfaces.wrapper.SystemSettingBox;
import com.yaf.framework.interfaces.wrapper.UiBox;
import com.yaf.framework.interfaces.wrapper.Waiter;

public class TS_HomeShell_HomeShellLauncher_Test extends BaseTest{
	Utils utils = new Utils();
	CommonOperation co = new CommonOperation();

	private String model = Utils.Model.hotRun.getName();
	public  String wifissid = ConfigParser.getProperty("wifiOfWpaCompanySsid1");
	public  String wifiidentify = ConfigParser.getProperty("wifiOfWpaCompanyName1");
	public  String wifipassword = ConfigParser.getProperty("wifiOfWpaCompanyPass1");
	@Before
	public void setUp() {
		Sleep.sleepSeconds(2);
		Assert.assertTrue("数据初始化:解锁屏幕", ScreenBox.unLock());
		Assert.assertTrue("初始测试环境: 连续点击back键(保证在桌面状态)", co.goback(5));
		Sleep.sleepSeconds(2);
		System.out.println("wifissid:" + wifissid);
		System.out.println("wifipassword:" + wifipassword);
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

	@After
	public void tearDown() {
		Sleep.sleepSeconds(2);
		Assert.assertTrue("清场测试环境: 连续点击back键(保证在桌面状态)", co.goback(5));
	}
	
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_001
	 * CaseID: 4209800
	 * Module: 桌面
	 * Title: 桌面呼出menu菜单后，点击【主题】菜单进入主题设置页面
	 * 优先级：P0:
	 * 前置条件：
	 * 步骤详情：
	 * 步骤1：桌面，点击menu键，选择【主题】选项
	 * 预期：切换到本地主题设置页面
	 * 
	 */
	@Test
	public void TC_testHomeShellLauncher_001() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				Assert.assertTrue("步骤1.1:点击Menu键", Keys.pressMenu());
				Sleep.sleepSeconds(2);
				
				int clickX = UiBox.getUiObject(new UiSelector().text("主题")).getCenterX();
				int clickY = UiBox.getUiObject(new UiSelector().text("主题")).getCenterY();
//				Assert.assertTrue("步骤1.2：点击主题选项", UiBox.);
//				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10);
				Assert.assertTrue("预期结果：进入到主题设置界面",
						Waiter.waitForPage(Constants.THEME_ACTIVITY));

				return 0;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
		
		
	}
	
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_002
	 * CaseID: 4209799
	 * Module: 桌面
	 * Title: 点击menu 键呼出桌面设置，检查菜单显示
	 * 优先级：P0:
	 * 前置条件：当前在桌面任意一屏
	 * 步骤详情：
	 * 步骤1：点击menu 键，检查菜单显示
	 * 预期：弹出menu菜单项，显示5个菜单项：主题、壁纸、小工具、桌面设置、设置      
	 * 步骤2：点击返回键
	 * 预期结果：回到之前所在屏
	 * 
	 */
	public void TC_testHomeShellLauncher_002() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				Assert.assertTrue("步骤1.1:点击Menu键", Keys.pressMenu());
				
//				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10);
				

				return 0;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
		
		
	}
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_003
	 * CaseID: 4209720
	 * Module: 桌面
	 * Title: 启动Dock区应用
	 * 优先级：P0:
	 * 前置条件：Dock区存在应用
	 * 步骤详情：
	 * 步骤1：点击Dock区某应用
	 * 预期：应用被成功启动 
	 * 步骤2：点击返回键
	 * 预期结果：回到之前所在屏
	 * 
	 */
	@Test
	public void TC_testHomeShellLauncher_003() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				
				UiObject obj = UiBox.getUiObject(new UiSelector().text("信息"));
				int clickX = obj.getCenterX();
				int clickY = obj.getCenterY();
				Assert.assertTrue("步骤1：点击信息",true);
				long reponseTime = co.clickAndWaitForBottomHalfScreenChanged(clickX, clickY);
				Assert.assertTrue("预期结果:信息界面被启动成功",Waiter.waitForPage(co.mmsActivity));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤2：点击返回键", Keys.pressBack());
				Assert.assertTrue("预期结果：返回桌面", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
//				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10);
				

				return reponseTime;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
		
		
	}
	
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_004
	 * CaseID: 4209720
	 * Module: 桌面
	 * Title: 启动widget并退出
	 * 优先级：P0:
	 * 前置条件：默认的第一屏有widget，天气、时间等
	 * 步骤详情：
	 * 步骤1：桌面页面点击widget图标
	 * 预期：该widget被成功启动   
	 * 步骤2：点击退出按键
	 * 预期结果：回到之前所在屏
	 * 
	 */
	public void TC_testHomeShellLauncher_004() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				Assert.assertTrue("数据初始化：设置自动更新时间", SystemSettingBox.setTimeAutoUpdate(true));
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				
				UiObject obj = UiBox.getUiObject(new UiSelector().textContains("月"));
				int clickX = obj.getCenterX();
				int clickY = obj.getCenterY();
				Assert.assertTrue("步骤1：点击日期widget",true);
				long reponseTime = co.clickAndWaitForBottomHalfScreenChanged(clickX, clickY);
				Assert.assertTrue("预期结果:日程界面被启动",Waiter.waitForPage(CommonOperation.calendarActivity));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤2：点击返回键", Keys.pressBack());
				Assert.assertTrue("预期结果：返回桌面", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
//				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10);
				

				return reponseTime;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
		
		
	}
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_005
	 * CaseID: 4237211
	 * Module: 桌面
	 * Title: 呼出语音识别模式
	 * 优先级：P0:
	 * 前置条件：手机网络正常
	 * 步骤详情：
	 * 步骤1：长按滑支持语音模式的icon2秒（电话、短信、联系人、设置、浏览器）
	 * 预期：呼出语音识别模式    
	 * 步骤2：点击退出按键
	 * 预期结果：退出语音
	 * 
	 */
	@Test
	public void TC_testHomeShellLauncher_005() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				
				Assert.assertTrue("步骤1：长按拨号",true);
				long reponseTime = co.longPressAndWaitForBottomHalfScreenChanged("拨号");
				Sleep.sleepSeconds(1);
				Assert.assertTrue("预期结果:弹出语音模式",true);
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤2：点击返回键", Keys.pressBack());
				Assert.assertTrue("预期结果：返回桌面", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
//				long reposeTime = co.clickAndWaitForScreenChangedandHoldtime(clickX, clickY, 2, 10);
				

				return reponseTime;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
		
		
	}
	
	/** @author xueya.hb
	 * TC_testHomeShellLauncher_006
	 * CaseID: 5509153
	 * Module: 桌面
	 * Title: 打开、关闭全局搜索
	 * 优先级：P0:
	 * 前置条件：非首次使用键盘
	 * 步骤详情：
	 * 步骤1：在桌面下滑屏幕（不能从状态栏处滑动）发起桌面搜索
	 * 预期：显示搜索框，并发起键盘（默认中文），有光标指示
     * 步骤2：点击空白处或按HOME键或按2次BACK键
     * 预期：回到桌面原先的屏幕中
     * 步骤3：再次下拉，发起全局搜索-&gt;点击取消按钮
     * 预期：搜索取消，回到桌面原先的屏幕
     * 步骤4：再次下拉，发起全局搜索
     * 预期：搜索结果展示
     * 步骤5：上下滑动展示界面
     * 预期：滑动流畅
     * 步骤6：按HOME键或按2次BACK键
     * 预期：回到桌面原先的屏幕中
	 * 
	 */
	@Test
	public void TC_testHomeShellLauncher_006() {
		String casename = Thread.currentThread().getStackTrace()[1].getMethodName();
		TestCaseModel testCaseModel = new TestCaseModel() {
			@Override
			public void BeforeRun() {
				// TODO Auto-generated method stub
				
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
				Assert.assertTrue("数据初始化：桌面处于第一屏", Keys.pressHome());
			}
			@Override
			public long CaseStep() {
				// TODO Auto-generated method stub
				int beginX = ScreenBox.getScreenWidth() /2;
				int beginY = ScreenBox.getScreenHeight() / 2;
				int endX = ScreenBox.getScreenWidth() / 2;
				int endY = ScreenBox.getScreenHeight();
				
				long responseTime = co.dragAndWaitForScreenChanged(beginX, beginY, endX, endY);
				Assert.assertTrue("步骤1：桌面下滑屏幕（不能从状态栏处滑动）发起桌面搜索", responseTime == -2?false:true);
				Sleep.sleepSeconds(1);
				Assert.assertTrue("预期结果1.1：显示搜索框，发起键盘（默认中文），有光标指示", UiBox.getUiObject(new UiSelector().text("取消")) == null?false:true);
				Sleep.sleepSeconds(1);
				if (UiBox.getUiObject(new UiSelector().textContains("欢迎使用")) != null) {
					UiBox.clickByText("欢迎使用");
					Sleep.sleepSeconds(1);
				}
//				Assert.assertTrue("预期结果1.2：发起键盘（默认中文），有光标指示", UiBox.getUiObject(new UiSelector().text("中")) == null?false:true);
//				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤2：按home键", Keys.pressHome());
				
				Assert.assertTrue("预期结果", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤3.1：再次下拉", UiBox.drag(beginX, beginY, endX, endY));
				Sleep.sleepSeconds(1);
				Keys.pressBack();//隐藏输入法
//				Assert.assertTrue("步骤3.2：输入全局搜索", UiBox.input("阿里巴巴"));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤3.2：点击取消按钮", UiBox.clickByText("取消"));
				
				Assert.assertTrue("预期结果：返回桌面", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤4.1：再次下拉", UiBox.drag(beginX, beginY, endX, endY));
				Sleep.sleepSeconds(1);
				Keys.pressBack();//隐藏输入法
				Assert.assertTrue("步骤4.2：输入全局搜索", UiBox.input("阿里巴巴"));
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤4.3：点击搜索", UiBox.clickByText("搜索"));
				
				Assert.assertTrue("预期结果：搜索结果展示", Waiter.waitForPage(Constants.SEARCH_PAGE_ACTIVITY));
				Sleep.sleepSeconds(5);
				Assert.assertTrue("步骤5：向上滑动屏幕", ScreenBox.swipeUp());
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤5：向下滑动屏幕", ScreenBox.swipeDown());
				Sleep.sleepSeconds(1);
				Assert.assertTrue("步骤6：按home键", Keys.pressHome());
				
				Assert.assertTrue("预期结果", Waiter.waitForPage(Constants.HOMESHELL_ACTIVITY));
				return responseTime;
			}
			
			@Override
			public void AfterRun() {
				// TODO Auto-generated method stub
				
			}
		};
		
		testCaseModel.testCaseHot(model, casename);
		
	}
	
	
}
