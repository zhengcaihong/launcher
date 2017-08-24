package com.aliyun.homeshell;

import java.io.File;
import java.util.Calendar;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

public class DataCollector {
    public static final String LABEL_WLAN = "wlan";
    public static final String LABEL_SOUND = "sound";
    public static final String LABEL_MOBILE_DATA = "mobiledata";
    public static final String LABEL_TIME_SETTING = "time";
    public static final String LABEL_MOBILESTATS_SETTING = "mobilestats";
    public static final String LABEL_LONGPRESS_EDIT_MODE = "edit_mode";
    public static final String LABEL_MENU_PRESSED = "menu";
    public static final String LABEL_MENU_THEME = "menu_theme";
    public static final String LABEL_MENU_WALLPAPER = "menu_wallpaper";
    public static final String LABEL_MENU_RINGTONE = "menu_ringtone";
    public static final String LABEL_MENU_SETTING = "menu_setting";

    public static final String PREFERENCES_CONFIG = "config";
    public static String PREFERENCES_FILE = "shared_prefs";//"/data/data/com.aliyun.homeshell/shared_prefs/";
    public static final String RUN_FIRST_TIME = "run_first_time";
    public static final String LAST_DOIC_TIME = "last_doic_time";
    
    public static final long SEVEN_DAY_MILLISECONDS = 604800000;
    // 7 * 24 * 60 * 60 * 1000;

    private static final String TAG = "DataCollector";

//    private static final String ACTION_TYPE = "action_type";
//    private static final String ACTION_TYPE_QUICK_SETTING = "quick_setting";
//    private static final String ACTION_TYPE_SCREEN_DATA = "screen_data";
//    private static final String ACTION_TYPE_DELETION_DATA= "deletion_data";
//    private static final String ACTION_TYPE_MENU_PRESSED = "menu_press";
//    private static final String ACTION_TYPE_ENTER_EDIT = "action_edit";
//
//    private static final String QUICK_SETTING_LABEL = "label";
//    private static final String QUICK_SETTING_DAYS = "days";
//    private static final String QUICK_SETTING_TIMES = "times";
//
//    private static final String SCREEN_DATA_SCREEN = "screen";
//    private static final String SCREEN_DATA_CONTAINER_ID = "container_id";
//    private static final String SCREEN_DATA_CELLX = "cellx";
//    private static final String SCREEN_DATA_CELLY = "celly";
//    private static final String SCREEN_DATA_TITLE = "title";
//    private static final String SCREEN_DATA_DATA_ID = "data_id";
//    
//    //deletion data
//    private static final String DELETION_TYPE = "deletion_type";
//    private static final String DELETION_TYPE_APP = "application";
//    private static final String DELETION_TYPE_SHORTCUT = "shortcut";
//    private static final String DELETION_TYPE_WIDGET = "widget";
//    private static final String DELETION_APP_NAME = "app_name";
//    private static final String DELETION_WIDGET_NAME = "widget_name";
//    private static final String DELETION_PACKAGE_NAME = "package_name";

    private static DataCollector mInstance = null;
    private static Context mContext;
    private SharedPreferences mSp;

    public enum WriteOp {Plus, Minus, Str};

    public static DataCollector getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DataCollector.class) {
                if (mInstance == null) {
                    mInstance = new DataCollector(context);
                }
            }
        }
        String path = context.getFilesDir().getAbsolutePath();
        if( !TextUtils.isEmpty(path) ){
            PREFERENCES_FILE = path.replace("files", "shared_prefs/");
        }
        return mInstance;
    }
    
    private DataCollector(Context context) {
        mContext = context;
        mSp = context.getSharedPreferences(PREFERENCES_CONFIG,
                Context.MODE_PRIVATE);
    }

    private void collectData(int days) {
        try {
            Log.e("ICDEBUG", "collectData");
            collectMobileClickData(days);
            collectWlanClickData(days);
            collectSoundClickData(days);
            collectTimeSettingData(days);
            collectMobileDataSettingData(days);
            collectEditModeData(days);
            collectMenuPressData(days,LABEL_MENU_PRESSED);
            collectMenuPressData(days,LABEL_MENU_THEME);
            collectMenuPressData(days,LABEL_MENU_WALLPAPER);
            collectMenuPressData(days,LABEL_MENU_RINGTONE);
            collectMenuPressData(days,LABEL_MENU_SETTING);
            collectDesktopItemPositionData();
        } catch (Exception e) {
            Log.d(TAG, "collectData, error:" + e.toString());
        }
        clear();
    }

    private void clear() {
        SharedPreferences sp = mSp;
        sp.edit().putInt(LABEL_MOBILE_DATA, 0).commit();
        sp.edit().putInt(LABEL_WLAN, 0).commit();
        sp.edit().putInt(LABEL_SOUND, 0).commit();
        sp.edit().putInt(LABEL_TIME_SETTING, 0).commit();
        sp.edit().putInt(LABEL_MOBILESTATS_SETTING, 0).commit();
        sp.edit().putInt(LABEL_LONGPRESS_EDIT_MODE, 0).commit();
        sp.edit().putInt(LABEL_MENU_PRESSED, 0).commit();
        sp.edit().putInt(LABEL_MENU_THEME, 0).commit();
        sp.edit().putInt(LABEL_MENU_WALLPAPER, 0).commit();
        sp.edit().putInt(LABEL_MENU_RINGTONE, 0).commit();
        sp.edit().putInt(LABEL_MENU_SETTING, 0).commit();
    }

    private void collectMobileClickData(int days) {
        // ICInfo ici = new ICInfo();
        //final int times_mobile = mSp.getInt(LABEL_MOBILE_DATA, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_QUICK_SETTING);
        // ici.add(QUICK_SETTING_LABEL, LABEL_MOBILE_DATA);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(times_mobile));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
        Log.d(TAG, "collectData, collectMobileClickData");
    }

    private void collectWlanClickData(int days) {
        // ICInfo ici = new ICInfo();
        //final int times_wlan = mSp.getInt(LABEL_WLAN, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_QUICK_SETTING);
        // ici.add(QUICK_SETTING_LABEL, LABEL_WLAN);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(times_wlan));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }

    private void collectSoundClickData(int days) {
        // ICInfo ici = new ICInfo();
        //final int times_sound = mSp.getInt(LABEL_SOUND, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_QUICK_SETTING);
        // ici.add(QUICK_SETTING_LABEL, LABEL_SOUND);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(times_sound));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }

    private void collectTimeSettingData(int days) {
        // ICInfo ici = new ICInfo();
        //final int times_time_setting = mSp.getInt(LABEL_TIME_SETTING, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_QUICK_SETTING);
        // ici.add(QUICK_SETTING_LABEL, LABEL_TIME_SETTING);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(times_time_setting));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }

    private void collectMobileDataSettingData(int days) {
        // ICInfo ici = new ICInfo();
        //final int times_mobile_setting = mSp.getInt(LABEL_MOBILESTATS_SETTING, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_QUICK_SETTING);
        // ici.add(QUICK_SETTING_LABEL, LABEL_MOBILESTATS_SETTING);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(times_mobile_setting));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }
    
    private void collectEditModeData(int days) {
        // ICInfo ici = new ICInfo();
        //final int enter_edit_mode_time = mSp.getInt(LABEL_LONGPRESS_EDIT_MODE, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_ENTER_EDIT);
        // ici.add(QUICK_SETTING_LABEL, LABEL_LONGPRESS_EDIT_MODE);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(enter_edit_mode_time));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }
    private void collectMenuPressData(int days, String prefString) {
        // ICInfo ici = new ICInfo();
        //int pressed_time = mSp.getInt(prefString, 0);
        // ici.add(ACTION_TYPE, ACTION_TYPE_MENU_PRESSED);
        // ici.add(QUICK_SETTING_LABEL, prefString);
        // ici.add(QUICK_SETTING_DAYS, String.valueOf(days));
        // ici.add(QUICK_SETTING_TIMES, String.valueOf(pressed_time));
        // ICHelper.wCustom(mContext, ici, mContext.getPackageName(), false);
    }

    private void collectDesktopItemPositionData() {
        final Context context = mContext;
        final ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);
        if (c != null) {
            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int titleIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.TITLE);
            final int containerIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.CONTAINER);
            final int screenIndex = c.getColumnIndexOrThrow(
                    LauncherSettings.Favorites.SCREEN);
            final int cellXIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.CELLX);
            final int cellYIndex = c.getColumnIndexOrThrow
                    (LauncherSettings.Favorites.CELLY);
            while (c.moveToNext()) {
                // ICInfo ici = new ICInfo();
                // ici.add(ACTION_TYPE, ACTION_TYPE_SCREEN_DATA);
                // ici.add(SCREEN_DATA_TITLE, c.getString(titleIndex));
                // ici.add(SCREEN_DATA_SCREEN, c.getString(screenIndex));
                // ici.add(SCREEN_DATA_CONTAINER_ID,
                // c.getString(containerIndex));
                // ici.add(SCREEN_DATA_CELLX, c.getString(cellXIndex));
                // ici.add(SCREEN_DATA_CELLY, c.getString(cellYIndex));
                // ici.add(SCREEN_DATA_DATA_ID, c.getString(idIndex));
                Log.d(TAG,
                        "addClickAction, collectDesktopItemPositionData, title:"
                                + c.getString(titleIndex)
                        + ", screen:" + c.getString(screenIndex)
                        + ", container_id:" + c.getString(containerIndex)
                        + ", cellX:" + c.getString(cellXIndex)
                        + ", cellY:" + c.getString(cellYIndex)
                        + ", data_id" + c.getString(idIndex));
                // ICHelper.wCustom(context, ici, context.getPackageName(),
                // false);
            }
            c.close();
        }
    }

    public void collectDeleteAppData(ShortcutInfo info){
        Log.d(TAG, "deleteApplication name=" + info.title);
        try{
            // ICInfo ici = new ICInfo();
            // ici.add(ACTION_TYPE, ACTION_TYPE_DELETION_DATA);
            // ici.add(DELETION_TYPE, DELETION_TYPE_APP);
            // ici.add(DELETION_APP_NAME, info.title);
            // ici.add(DELETION_PACKAGE_NAME,
            // ShortcutInfo.getPackageName(info.intent));
            // ICHelper.wCustom(mContext, ici, mContext.getPackageName(),
            // false);
        }catch(LinkageError ex){
            
        }
    }
    
    public void collectDeleteWidgetData(LauncherAppWidgetInfo info){
        Log.d(TAG, "DeleteWidget name=" + info.title);
        try{
            // ICInfo ici = new ICInfo();
            // ici.add(ACTION_TYPE, ACTION_TYPE_DELETION_DATA);
            // ici.add(DELETION_TYPE, DELETION_TYPE_WIDGET);
            // ici.add(DELETION_WIDGET_NAME, info.title);
            // ici.add(DELETION_PACKAGE_NAME,
            // info.providerName.getPackageName());
            // ICHelper.wCustom(mContext, ici, mContext.getPackageName(),
            // false);
        }catch(LinkageError ex){
            
        }
    }
    
    public void collectDeleteShortcutData(ShortcutInfo info){
        try{
            Log.d(TAG, "DeleteShortcut name=" + info.title);
            // ICInfo ici = new ICInfo();
            // ici.add(ACTION_TYPE, ACTION_TYPE_DELETION_DATA);
            // ici.add(DELETION_TYPE, DELETION_TYPE_SHORTCUT);
            // ici.add(DELETION_APP_NAME, info.title);
            // ici.add(DELETION_PACKAGE_NAME,
            // ShortcutInfo.getPackageName(info.intent));
            // ICHelper.wCustom(mContext, ici, mContext.getPackageName(),
            // false);
        }catch(LinkageError ex){
            
        }
    }

    public void addClickAction(String label) {
        ensureICFileIsExist();
        SharedPreferences sp = mSp;
        int curTimes = 0;
        if (LABEL_MOBILE_DATA.equals(label)) {
            curTimes = sp.getInt(LABEL_MOBILE_DATA, 0);
            sp.edit().putInt(LABEL_MOBILE_DATA, ++curTimes).commit();
        } else if (LABEL_WLAN.equals(label)) {
            curTimes = sp.getInt(LABEL_WLAN, 0);
            sp.edit().putInt(LABEL_WLAN, ++curTimes).commit();
        } else if (LABEL_SOUND.equals(label)) {
            curTimes = sp.getInt(LABEL_SOUND, 0);
            sp.edit().putInt(LABEL_SOUND, ++curTimes).commit();
        } else if (LABEL_TIME_SETTING.equals(label)) {
            curTimes = sp.getInt(LABEL_TIME_SETTING, 0);
            sp.edit().putInt(LABEL_TIME_SETTING, ++curTimes).commit();
        } else if (LABEL_MOBILESTATS_SETTING.equals(label)) {
            curTimes = sp.getInt(LABEL_MOBILESTATS_SETTING, 0);
            sp.edit().putInt(LABEL_MOBILESTATS_SETTING, ++curTimes).commit();
        }else if(LABEL_LONGPRESS_EDIT_MODE.equals(label)){
            curTimes = sp.getInt(LABEL_LONGPRESS_EDIT_MODE, 0);
            sp.edit().putInt(LABEL_LONGPRESS_EDIT_MODE, ++curTimes).commit();
        }else if(LABEL_MENU_PRESSED.equals(label)){
            curTimes = sp.getInt(LABEL_MENU_PRESSED, 0);
            sp.edit().putInt(LABEL_MENU_PRESSED, ++curTimes).commit();
        }else if(LABEL_MENU_THEME.equals(label)){
            curTimes = sp.getInt(LABEL_MENU_THEME, 0);
            sp.edit().putInt(LABEL_MENU_THEME, ++curTimes).commit();
        }else if(LABEL_MENU_WALLPAPER.equals(label)){
            curTimes = sp.getInt(LABEL_MENU_WALLPAPER, 0);
            sp.edit().putInt(LABEL_MENU_WALLPAPER, ++curTimes).commit();
        }else if(LABEL_MENU_RINGTONE.equals(label)){
            curTimes = sp.getInt(LABEL_MENU_RINGTONE, 0);
            sp.edit().putInt(LABEL_MENU_RINGTONE, ++curTimes).commit();
        }else if(LABEL_MENU_SETTING.equals(label)){
            curTimes = sp.getInt(LABEL_MENU_SETTING, 0);
            sp.edit().putInt(LABEL_MENU_SETTING, ++curTimes).commit();
        }
        Log.d(TAG, "addClickAction, label:" + label + ", curTimes:" + curTimes);
    }

    public void doIC(Context context, int days) {
        ensureICFileIsExist();
        collectData(days);
        updateSendFlag();
    }
    
    public void updateSendFlag() {
        StringBuilder curTimeFormat = new StringBuilder();
        long time = System.currentTimeMillis();
        Calendar calendar=Calendar.getInstance();
        calendar.clear();
        calendar.setTimeInMillis(time);
        String dateStr = DateFormat.getDateFormat(mContext).format(calendar.getTime());
        curTimeFormat.append(dateStr).append(" ");
        String timeStr = DateFormat.getTimeFormat(mContext).format(calendar.getTime());
        curTimeFormat.append(timeStr).append(" ");
        
        SharedPreferences icSharedP = mContext.getSharedPreferences(
                PREFERENCES_CONFIG, Context.MODE_PRIVATE);
        icSharedP.edit().putLong(LAST_DOIC_TIME, time).commit();
        icSharedP.edit().putString("LAST_SEND_TIME_human", curTimeFormat.toString()).commit();
    }
    public boolean ensureICFileIsExist() {
        File icFile = new File(PREFERENCES_FILE);
        if (!icFile.exists()) {
            updateSendFlag();
            return false;
        }
        return true;
    }
}
