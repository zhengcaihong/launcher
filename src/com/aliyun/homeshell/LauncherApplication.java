/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.homeshell;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import yunos.ui.util.DynColorSetting;
import app.aliyun.content.res.ThemeResources;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;

import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.appgroup.AppGroupManager;
import com.aliyun.homeshell.backuprestore.BackupManager;
import com.aliyun.homeshell.backuprestore.BackupRecord;
import com.aliyun.homeshell.backuprestore.BackupUitil;
import com.aliyun.homeshell.editmode.EditModeHelper;
import com.aliyun.homeshell.editmode.WidgetPreviewLoader;
import com.aliyun.homeshell.favorite.RecommendTask;
import com.aliyun.homeshell.gadgets.HomeShellGadgetsRender;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.setting.ContinuousHomeShellReceiver;
import com.aliyun.homeshell.setting.HomeShellSetting;
/*YUNOS BEGIN*/
//##date:2013/12/20 ##author:hao.liuhaolh ##BugID: 75596
// restore app isn't in folder if restore failed and reload.
/*YUNOS END*/

import com.aliyun.homeshell.utils.Utils;
import com.aliyun.homeshell.IconDigitalMarkHandler;
import com.aliyun.utility.FeatureUtility;
import java.util.ArrayList;

import com.mediatek.common.featureoption.XunhuOption;
import java.lang.reflect.Method;

public class LauncherApplication extends Application implements
        ThemeChangedListener.IThemeChanged, FontChangedListener.IFontChanged {
    private LauncherModel mModel;
    private IconManager mIconManager = null;
    private WidgetPreviewLoader.CacheDb mWidgetPreviewCacheDb;
    private static boolean sIsScreenLarge;
    private static float sScreenDensity;
    private static int sScreenWidth = 0;
    private static int sScreenHeight = 0;
    private static int sLongPressTimeout = 300;
    private static final String sSharedPreferencesKey = "com.aliyun.homeshell.prefs";
    private static final String TAG = "LauncherApplication";
    private boolean mIsCollectFavoriteData = false;
    public static final int SETTINGS_AGED_MODE = 1;

    private CheckVoiceCommandPressHelper mVuiHelper;
    
    private static int sConfigOrientation = Configuration.ORIENTATION_PORTRAIT;

    /*YUNOS BEGIN*/
    //##date:2013/12/20 ##author:hao.liuhaolh ##BugID: 75596
    // restore app isn't in folder if restore failed and reload.
    private static final String RESTORE_DB_FILE = "restore.db";
    /*YUNOS END*/

    WeakReference<LauncherProvider> mLauncherProvider;
    //edit by dongjun for traffic panel begin
    private static Context mContext = null;;
    //edit by dongjun for traffic panel end
    public static Launcher mLauncher = null;
    public static HomeShellSetting homeshellSetting = null;
    public static HashMap<String, BackupRecord> mBackupRecordMap; 

    //(BugID:134985 by wenliang.dwl) for desktop continuous feature
    private ContinuousHomeShellReceiver mContinuousHomeShellReceiver;

    /*YUNOS BEGIN*/
    //##date:2014/8/1 ##author:zhangqiang.zq
    // favorite app
    private RecommendTask mRecommendTask;

    private static final String LAST_SEND_CONFIGURATION_DAY = "LAST_SEND_CONFIGURATION_DAY";

    private DynColorSetting mDynColorSetting;

    private static final int INVALID_DAY = -1;
    private static final int MAX_DAYS = 3;
    private static final String DAY_COUNT = "DAY_COUNT";
    private static final String DAY_RECODER = "DAY_RECORD";
    private final BroadcastReceiver mNetWorkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivityManager = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if(info != null && info.isAvailable()) {
                    SharedPreferences sp = getSharedPreferences(
                            DataCollector.PREFERENCES_CONFIG, Context.MODE_PRIVATE);
                    if (sp != null) {
                        sp.edit().putInt(DAY_COUNT, 0).commit();
                        sendDayAppsData();
                        unregisterReceiver(mNetWorkReceiver);
                    }
                }
            }
        }};

    public void collectUsageData(long appId) {
        if (mIsCollectFavoriteData) {
            mRecommendTask.notifyAppClicked(appId);
        }
    }
    /* YUNOS END */

    /* YUNOS BEGIN */
    //##date:2013/11/28 ##author:hongxing.whx
    //backup restore functionality support
    private void handleRestore() {
        if (BackupManager.getRestoreFlag(this)) {
            //set in restore flag, so that other part can use it to judge if homeshell
            //is in restore mode
            Log.d(TAG, "Set homeshell inRestore flag");
            BackupManager.getInstance().setIsInRestore(true);
//            BackupManager.setRestoreFlag(this, false);

            /*YUNOS BEGIN*/
            //##date:2013/12/20 ##author:hao.liuhaolh ##BugID: 75596
            // restore app isn't in folder if restore failed and reload.
            Cursor c = null;
            File restoreDBfile = new File(getApplicationContext().getFilesDir() 
                                    + "/backup/" + RESTORE_DB_FILE);
            if (restoreDBfile.exists() == true) {
                Log.d(TAG, "handleRestore read data from restore db");
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(restoreDBfile, null);
                c = db.query("favorites", null, null, null, null, null, null);
                //convertDBToBackupSet will close cursor, so we don't need to close it
                mBackupRecordMap = BackupUitil.convertDBToBackupSet(c);
                db.close();
            } else {
                Log.d(TAG, "handleRestore read data from homeshell db");
                final ContentResolver contentResolver = getApplicationContext().getContentResolver();
                c = contentResolver.query(Favorites.CONTENT_URI, null, null, null, null);
                //convertDBToBackupSet will close cursor, so we don't need to close it
                mBackupRecordMap = BackupUitil.convertDBToBackupSet(c);
            }
            /*YUNOS END*/

            for (Entry<String, BackupRecord> r : LauncherApplication.mBackupRecordMap.entrySet()) {
                Log.d(TAG, r.getValue().getField(Favorites._ID));
                String intentStr = r.getValue().getField(Favorites.INTENT);
                if (TextUtils.isEmpty(intentStr)) {
                    continue;
                }
                try {
                    Intent intent = Intent.parseUri(intentStr, 0);
                    final ComponentName name = intent.getComponent();
                    if (name == null) {
                        Log.e(TAG, "ComponentName == Null");
                        Log.i(TAG, "intent = " + intent.toString());
                        continue;
                    }
                    Log.d(TAG, "onCreate() mBackupRecordMap getPackageName()=" + intent.getComponent().getPackageName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /* YUNOS END */
    @Override
    public void onCreate() {
        mDynColorSetting = new DynColorSetting(getResources().getConfiguration());
        DynColorSetting.setColorIDReady(getResources(), this.getPackageName());
        overlayDynColorRes(getResources(), mDynColorSetting,this.getPackageName());
        super.onCreate();
        //edit by dongjun for traffic panel begin
        mContext = getApplicationContext();
        //edit by dongjun for traffic panel end
        sConfigOrientation = getResources().getConfiguration().orientation;
        /* YUNOS BEGIN */
        //##date:2014/04/15 ##author:nater.wg ##BugID:110407
        //Enhance ConfigManager.
        ConfigManager.init();
        /* YUNOS END */
 
        /* YUNOS BEGIN */
        //##date:2013/11/28 ##author:hongxing.whx
        //backup restore functionality support
        handleRestore();
        /* YUNOS END */

        // set sIsScreenXLarge and sScreenDensity *before* creating icon cache
        try {
            sIsScreenLarge = getResources().getBoolean(R.bool.is_large_screen);
        } catch (Exception e) {
            sIsScreenLarge = false;
        }
        Log.d(TAG, "sIsScreenLarge="+sIsScreenLarge);
        sScreenDensity = getResources().getDisplayMetrics().density;
        sScreenWidth = getResources().getDisplayMetrics().widthPixels;
        sScreenHeight = getResources().getDisplayMetrics().heightPixels;

        mWidgetPreviewCacheDb = new WidgetPreviewLoader.CacheDb(this);
        mIconManager = new IconManager(this);
        mModel = new LauncherModel(this);
        mContinuousHomeShellReceiver = new ContinuousHomeShellReceiver(this);

        // Register intent receivers
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
         filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        filter.addDataScheme("package");
        registerReceiver(AppDownloadManager.getInstance().getPackageStateReceiver(), filter);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(LauncherModel.ACTION_APP_LAUNCHED);
        registerReceiver(mModel, filter);

        //add by dongjun for appstore begin
        filter = new IntentFilter();
        filter.addAction(AppDownloadManager.ACTION_APP_DWONLOAD_TASK);
        registerReceiver(mModel, filter);
        //add by dongjun for appstore end

        // added by wenliang.dwl for broadcast send from homeshell
        filter = new IntentFilter();
        filter.addAction(AppDownloadManager.ACTION_HS_DOWNLOAD_TASK);
        registerReceiver(mModel, filter);
        
        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if(FeatureUtility.hasAgedModeFeature()){
            filter = new IntentFilter();
            filter.addAction(LauncherModel.ACTION_AGED_MODE_CHANGED);
            registerReceiver(mModel, filter);
        }
        /*YUNOS END*/

        //for layout change
        filter = new IntentFilter();
        filter.addAction(LauncherModel.ACTION_HOMESHELL_LAYOUT_CHANGE);
        /* YUNOS BEGIN */
        // ##date:2014/06/05 ##author:hongchao.ghc ##BugID:126343
        // for settings show icon mark
        filter.addAction(LauncherModel.ACTION_UPDATE_LAYOUT);
        /* YUNOS END */
        filter.addAction(LauncherModel.ACTION_UPDATE_SLIDEUP);
        filter.addAction(LauncherModel.ACTION_UPDATE_CLONABLE);
        filter.addAction(LauncherModel.ACTION_REMOVE_APP_VIEWS);
        registerReceiver(mModel, filter);

        //(BugID:134985 by wenliang.dwl) for desktop continuous feature
        filter = new IntentFilter();
        filter.addAction(ContinuousHomeShellReceiver.CONTINUOUS_HOMESHELL_SHOW_ACTION);
        registerReceiver(mContinuousHomeShellReceiver, filter);

        //bugid: 5232096: register ACTION_APPLICATION_NOTIFICATION intent
        filter = new IntentFilter();
        filter.addAction(IconDigitalMarkHandler.ACTION_APPLICATION_NOTIFICATION);
        registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(HomeShellSetting.ACTION_ON_MARK_TYPE_CHANGE);
        registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(HomeShellSetting.ACTION_ON_SHOW_NEW_MARK_CHANGE);
        registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(HomeShellSetting.ACTION_ON_SHOW_CLONABLE_MARK_CHANGED);
        registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(HomeShellSetting.ACTION_ON_SHOW_SLIDE_UP_MARK_CHANGE);
        registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(HomeShellSetting.ACTION_HOTSEAT_LAYOUT_CHANGE);
        registerReceiver(mModel, filter);

        SharedPreferences sp = getSharedPreferences(
                DataCollector.PREFERENCES_CONFIG, Context.MODE_PRIVATE);
        if (sp != null && sp.getInt(DAY_COUNT, INVALID_DAY) == INVALID_DAY) {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if(info != null && info.isAvailable()) {
                sp.edit().putInt(DAY_COUNT, 0).commit();
            } else {
                filter = new IntentFilter();
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                registerReceiver(mNetWorkReceiver, filter);
            }
        }

        IconDigitalMarkHandler.getInstance();
        //end of 5232096

        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);

        /* YUNOS BEGIN */
        // ##date:2013/11/20 ##author:zhangqiang.zq
        // add theme icon
        ThemeChangedListener.getInstance(getApplicationContext()).register(
                getApplicationContext());
        ThemeChangedListener.getInstance(getApplicationContext()).addListener(
                this);
        /* YUNOS END */
        
        FontChangedListener.getInstance(getApplicationContext()).register(getApplicationContext());
        FontChangedListener.getInstance(getApplicationContext()).addListener(this);

        /*YUNOS BEGIN added by xiaodong.lxd for push to talk*/
        CheckVoiceCommandPressHelper.checkEnvironment();
        if(CheckVoiceCommandPressHelper.PUSH_TO_TALK_SUPPORT) {
            mVuiHelper = CheckVoiceCommandPressHelper.getInstance();
            filter = new IntentFilter();
            filter.addAction(CheckVoiceCommandPressHelper.BROADCAST_PUSHTALK_SWITCH_CHANGED);
            registerReceiver(mVuiHelper.mSwitchReceiver, filter);
            if(mVuiHelper.isVoiceSwitchOn()) {
                mVuiHelper.initVoiceService();
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/1/9 ##author:zhangqiang.zq
        //smart search
        //##date:2015/2/2 ##author:zhanggong.zg ##BugID:5719824
        //smart search is disabled to reduce initial memory usage
        /*
        HanziToPinyin.getInstance().initHanziPinyinForAllChars(getContext());
        */
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/8/1 ##author:zhangqiang.zq
        // favorite app
        mRecommendTask = new RecommendTask(getApplicationContext());
        /*YUNOS END*/
        if (FeatureUtility.isYunOSInternational() || FeatureUtility.isYunOSForCTA()) {
            AppGroupManager.switchOff();
        } else {
            AppGroupManager.switchOn();
        }
        if (AppGroupManager.isSwitchOn()) {
            AppGroupManager manager = AppGroupManager.getInstance();
            manager.initAppInfos();
            if (!manager.isLoadedSuccess()) {
                LauncherModel
                        .startLoadAppGroupInfo(AppGroupManager.DELAY_TIME_LONG);
            }
        }

        Launcher.mIsAgedMode = SETTINGS_AGED_MODE == Settings.Secure.getInt(
                getContext().getContentResolver(), "aged_mode", 0);
        checkOSUpdate();
    }

    public void checkOSUpdate(){
        Intent newIntent = new Intent("com.yunos.fota.action.AppUpdateService");
        newIntent.setPackage("com.aliyun.fota");//targetSDK version is Android SDK 5.0 or later, have to add this line, or exception thrown!
        newIntent.putExtra("PackageName", "com.aliyun.homeshell");
        newIntent.putExtra("AutoCheckType","AutoCheckInWifi");
        startService(newIntent);
    }
    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();

        unregisterReceiver(mModel);
        //(BugID:134985 by wenliang.dwl) for desktop continuous feature
        unregisterReceiver(mContinuousHomeShellReceiver);

        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);

        /* YUNOS BEGIN */
        // ##date:2013/11/20 ##author:zhangqiang.zq
        // add theme icon
        ThemeChangedListener.getInstance(getApplicationContext()).unregister(
                getApplicationContext());
        /* YUNOS END */
        
        FontChangedListener.getInstance(getApplicationContext()).unregister(getApplicationContext());
        
        //edit by dongjun for traffic panel begin
        this.mModel.destroy();
        mContext = null;
        //edit by dongjun for traffic panel end
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        mModel.initialize(launcher);
        mLauncher = launcher;
        return mModel;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public WidgetPreviewLoader.CacheDb getWidgetPreviewCacheDb() {
        return mWidgetPreviewCacheDb;
    }

   void setLauncherProvider(LauncherProvider provider) {
        mLauncherProvider = new WeakReference<LauncherProvider>(provider);
    }

    public LauncherProvider getLauncherProvider() {
        return mLauncherProvider.get();
    }

    public static String getSharedPreferencesKey() {
        return sSharedPreferencesKey;
    }

    public static boolean isScreenLarge() {
        return sIsScreenLarge;
    }

    public static boolean isScreenLandscape(Context context) {
        return context.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
    }

    public static float getScreenDensity() {
        return sScreenDensity;
    }
    
    public static int getScreenWidth() {
        return sScreenWidth;
    }
    
    public static int getScreenHeight() {
        return sScreenHeight;
    }
    
    public static int getLongPressTimeout() {
        return AgedModeUtil.isAgedMode() ? 1000 : sLongPressTimeout;
    }
    
    public IconManager getIconManager(){
    	return mIconManager;
    }
    @Override
    public void onThemeChanged() {
        if (!EditModeHelper.isChangeThemeFromeHomeShell()) {
            Utils.showLoadingDialog(mLauncher, R.string.theme_loading);
        }
        /*mModel.resetLoadedState(false, true);
        //##date:2013/12/08 ##author:hongxing.whx ##bugid: 72248
        mModel.setThemeChanged(true);
        YUNOS END
        mModel.startLoaderFromBackground();*/

        /*YUNOS BEGIN*/
        //##date:2014/8/1 ##author:zhangqiang.zq
        // favorite app
        GadgetCardHelper.onThemeChanged();
        LauncherGadgetHelper.cleanUp();
        mRecommendTask.refreshFavoriteAppIcons();
        /* YUNOS END */
        ThemeResources.reset();
        mModel.onThemeChange();
        if (mLauncher != null) {
            mLauncher.onThemeChanged();
        }
        HomeShellGadgetsRender.getRender().onThemeChange();
        LauncherAnimUtils.onDestroyActivity();
    }
    //edit by dongjun for traffic panel begin
    public static Context getContext(){
    	return mContext;
    }
    //edit by dongjun for traffic panel end
    public static Launcher getLauncher() {
        return mLauncher;
    }

    @Override
    public void onFontChanged() {
        mModel.onFontChanged();
    }

    public void sendDayAppsData() {
        final SharedPreferences sp = getSharedPreferences(
                DataCollector.PREFERENCES_CONFIG, Context.MODE_PRIVATE);
        final int dayCount = sp.getInt(DAY_COUNT, INVALID_DAY);
        if (dayCount > INVALID_DAY && dayCount < MAX_DAYS) {
            int day = sp.getInt(DAY_RECODER, 0);
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int date = c.get(Calendar.DATE);
            final int hash = year * 1000 + month * 100 + date;
            if (day != hash) {

                new Thread() {

                    @Override
                    public void run() {
                        try {
                            // delay 10s waiting for the IC service to start when
                            // booted
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ArrayList<ItemInfo> list = LauncherModel.getAllAppItems();
                        StringBuilder sb = new StringBuilder(200);
                        boolean inFolder = false;
                        for (ItemInfo item : list) {
                            if (!(item instanceof ShortcutInfo)) {
                                continue;
                            }
                            if (item.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                                    && item.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING
                                    && item.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
                                continue;
                            }
                            ShortcutInfo info = (ShortcutInfo)item;
                            sb.append(info.getPackageName()); // packageName
                            sb.append('_');
                            inFolder = info.container >= 0;
                            if (inFolder) {
                                FolderInfo folderInfo = Launcher.findFolderInfo(info.container);
                                if (folderInfo == null) {
                                    Log.e(TAG,
                                            "ERROR! sendDayAppsData failed, findFolderInfo() == null info:"
                                                    + info);
                                    return;
                                }
                                sb.append(folderInfo.screen + 1); // screen index
                                sb.append('_');
                                sb.append(folderInfo.cellY + 1); // cell Y
                                sb.append('_');
                                sb.append(folderInfo.cellX + 1); // cell X
                                sb.append('_');
                                sb.append(1); // in folder
                                sb.append('_');
                                sb.append(info.screen + 1); // screen index of folder
                            } else {
                                sb.append(info.screen + 1); // screen index
                                sb.append('_');
                                sb.append(info.cellY + 1); // cell Y
                                sb.append('_');
                                sb.append(info.cellX + 1); // cell X
                                sb.append('_');
                                sb.append(0); // not in folder
                            }
                            sb.append(',');
                        }
                        if (sb.length() > 1) {
                            sb.deleteCharAt(sb.length() - 1);
                            UserTrackerHelper.dayAppStatus(dayCount, sb.toString());
                            sp.edit().putInt(DAY_RECODER, hash).commit();
                            sp.edit().putInt(DAY_COUNT, dayCount + 1).commit();
                        }
                }}.start();
            }
        }
    }

    public void sendConfigurationData() {
        SharedPreferences sp = getSharedPreferences(
                DataCollector.PREFERENCES_CONFIG, Context.MODE_PRIVATE);
        final int lastDate = sp.getInt(LAST_SEND_CONFIGURATION_DAY, INVALID_DAY);
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date = c.get(Calendar.DATE);
        int hash = year * 1000 + month * 100 + date;
        if (lastDate != hash) {
            sp.edit().putInt(LAST_SEND_CONFIGURATION_DAY, hash).commit();
            new Thread() {

                @Override
                public void run() {
                    try {
                        // delay 10s waiting for the IC service to start when
                        // booted
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Map<String, String> param = new HashMap<String,String>();
                    param.put(UserTrackerMessage.Key.IF_CROSS_SCREEN, ConfigManager.isLandOrienSupport()+"");
                    UserTrackerHelper.sendCustomEvent(UserTrackerMessage.MSG_CONFIGURATION, param);
            }}.start();
        }
    }

    public void overlayDynColorRes(Resources res, DynColorSetting dynColorSetting, String pakName) {
        DynColorSetting.clearNewResArray(res);
        if ( !dynColorSetting.isRestoreMode() ) {
            SparseIntArray newcolors = new SparseIntArray();
            int bgcolor = dynColorSetting.getColorValue(DynColorSetting.HEADER_COLOR,getResources().getColor(R.color.setting_header_color));
            newcolors.put(R.color.setting_header_color, bgcolor);
            int textcolor = dynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR, getResources().getColor(R.color.setting_title_color));
            newcolors.put(R.color.setting_title_color, textcolor);
            int mode = dynColorSetting.getDarkMode(getResources().getBoolean(R.bool.setting_dark_mode));
            newcolors.put(R.bool.setting_dark_mode, mode);
            int widgetcolornormal = dynColorSetting.getColorValue(DynColorSetting.HEADER_WIDGET_NORMAL, getResources().getColor(R.color.back_color));
            newcolors.put(R.color.back_color, widgetcolornormal);
            int widgetcolorpressed = dynColorSetting.getColorValue(DynColorSetting.HEADER_WIDGET_PRESSED, getResources().getColor(R.color.back_color_dark));
            newcolors.put(R.color.back_color_dark, widgetcolorpressed);
            int widgetcolordisable = dynColorSetting.getColorValue(DynColorSetting.HW_BUTTON_COLOR_DISABLE, getResources().getColor(R.color.back_color_disabled));
            newcolors.put(R.color.back_color_disabled, widgetcolordisable);
            int edgeeffectcolor = getEdgeEffectColor(dynColorSetting.getColorValue(DynColorSetting.HEADER_COLOR,getResources().getColor(R.color.edge_effect_color)));
            newcolors.put(R.color.edge_effect_color, edgeeffectcolor);
            DynColorSetting.setNewResArray(res,  DynColorSetting.setPrimaryColor(res, dynColorSetting, newcolors,
                    pakName));
        }
    }


    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    public void onAgedModeChanged(boolean agedMode, boolean forceChangeLayout, boolean forceLoad) {
        if (agedMode == AgedModeUtil.isAgedMode() && !forceChangeLayout) {
            return;
        }
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        if (preference == null) {
            return;
        }
        // update global search visibility when has global search
        if (mLauncher != null) {
           mLauncher.setGlobalSearchVisibility(agedMode ? View.GONE: View.VISIBLE);
        }
        //HomeShellSetting.setFreezeValue(LauncherApplication.getContext(), agedMode);
        HomeShellSetting.setLayoutValue(LauncherApplication.getContext(), agedMode);
        String current = preference.getString(HomeShellSetting.KEY_PRE_LAYOUT_STYLE, "");

        int countX = 4;
        int countY = 4;
        //0 : 4 x 4
        //1 : 4 x 5
        //2 : 3 x 3, for agedmode
        if(current.equals("0")){
            countX = 4;
            countY = 4;
        } else if (current.equals("1")) {
            countX = 4;
            countY = 5;
        } else if (current.equals("2")) {
            countX = 3;
            countY = 3;
        }else{
            countX = 4;
            countY = 4;
        }

        mModel.changeLayoutForAgedModeChanged(agedMode, countX, countY);
        if (homeshellSetting != null) {
            homeshellSetting.updateLayoutPreference();
        }
        mModel.switchDbForAgedMode(agedMode);
        mModel.clearDownloadItems();
        if (forceLoad) {
            mModel.resetLoadedState(true, true);
            mModel.startLoader(false, -1);
        }
    }
    /*YUNOS END*/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if( !mDynColorSetting.isSameColorMap(newConfig) ) {
            mDynColorSetting.updateColorMap(newConfig);
            overlayDynColorRes(getResources(), mDynColorSetting, this.getPackageName());
            /* YUNOS_BEGIN */
            // ##modules(HomeShell): add for dynamic color of Card
            // ##date: 2015-11.10 author: ruijie.lrj@alibaba-inc.com
            GadgetCardHelper.onColorChanged(mContext, mDynColorSetting);
            /* YUNOS_END */
        }
        super.onConfigurationChanged(newConfig);
        if (!ConfigManager.isLandOrienSupport()) {
            return;
        }
    	if(newConfig.orientation != sConfigOrientation) {
            sConfigOrientation = newConfig.orientation;
            if(mModel.isLoadingWorkspace()) {
    	        mModel.addWaitingBindForOrienChanged();
                return;
            }
            ConfigManager.reCreateConfigDataOnOrientationChanged();
            Log.d(TAG, "sxsexe_test    onConfigurationChanged ConfigManager.toString " + ConfigManager.toLogString());
            mModel.resetDataOnConfigurationChanged();
    	}
    }
    
    public static boolean isInLandOrientation() {
        if(ConfigManager.isLandOrienSupport()) {
            if(mLauncher != null) {
                sConfigOrientation = mLauncher.getResources().getConfiguration().orientation;
            }
            return sConfigOrientation == Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return false;
        }
    }

    private int getEdgeEffectColor(int headercolor){
        int color;
        if(DynColorSetting.isGreyColor(headercolor)){
            color = getApplicationContext().getResources().getColor(R.color.edge_effect_color);
        }else{
            color = headercolor;
        }
        return color;
    }
}
