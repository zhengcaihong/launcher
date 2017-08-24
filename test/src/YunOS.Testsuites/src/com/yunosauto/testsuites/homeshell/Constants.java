package com.yunosauto.testsuites.homeshell;

import com.yaf.framework.interfaces.wrapper.ScreenBox;

public class Constants {
	public static final String SETTINGS_ACTIVITY = "com.android.settings/com.android.settings.Settings";
	
	public static final String HOMESHELL_ACTIVITY = "com.aliyun.homeshell/com.aliyun.homeshell.Launcher";
	
	public static final String QUICK_SETTING_ACTIVITY = "com.android.systemui/com.android.systemui.quickpannel.QuickPannelSettingActivity";
	
	public static final String THEME_ACTIVITY = "com.yunos.theme.thememanager/com.yunos.theme.thememanager.main.ThemeMainActivity";
	
	public static final String SEARCH_PAGE_ACTIVITY = "com.yunos.alimobilesearch/com.yunos.alimobilesearch.MSearchResultPageNew";
	
	public static final int STATUSBAR_START_X = ScreenBox.getScreenWidth() / 2;
	
	public static final int STATUSBAR_START_Y = ScreenBox.getStatusBarHeight()/2;
	
	public static final int STATUSBAR_END_X = STATUSBAR_START_X;
	
	public static final int STATUSBAR_END_Y = ScreenBox.getScreenHeight();
	
	public static final String SOUND = "响铃";
	
	public static final String VIBRATE = "振动";
	
	public static final String SILENT = "静音";
	
	public static final String GPS = "定位";
	
	public static final String MEETING_MODE = "会议";
	
	
}
