package com.aliyun.homeshell.setting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import storeaui.app.HWPreferenceActivity;
import storeaui.preference.HWSwitchPreference;
import storeaui.widget.ActionSheet;
import storeaui.widget.ActionSheet.SingleChoiceListener;
import yunos.ui.util.ReflectHelper;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.ConfigManager;
import com.aliyun.homeshell.Launcher;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.R;
import com.aliyun.homeshell.UserTrackerHelper;
import com.aliyun.homeshell.UserTrackerMessage;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.icon.IconManager;
import com.aliyun.homeshell.lifecenter.CardBridge;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.utils.Utils;
import com.aliyun.utility.FeatureUtility;
import com.aliyun.utility.utils.ACA;

public class HomeShellSetting extends HWPreferenceActivity implements
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener{
        //ThemeChangedListener.IThemeChanged{
    /* YUNOS BEGIN */
    // ##date:2014/06/05 ##author:hongchao.ghc
    // add luancher setting Buried
    private static final String TAG = "HomeShellSetting";
    private PreferenceScreen mContainer;

    private SharedPreferences mSharedRef;
    private Preference mLayoutPref = null;
    private HWSwitchPreference mContinuousHomePref;
    private int mOldLayoutMode = 0;
    private HWSwitchPreference mShowNewMarkIconPref = null;
    private HWSwitchPreference mShowSlideUpMarkIconPref = null;
    private HWSwitchPreference m3rdAppNotificationPrefNew = null;
    private HWSwitchPreference mShowClonableMarkIconPref = null;
    private String[] mLayoutTitle;
    private String[] mLayoutValue;

    public static final String KEY_PRE_EFFECT_STYLE = "effect_preference";
    public static final String KEY_PRE_LAYOUT_STYLE = "layout_preference";
    public static final String KEY_NOTIFICATION_MARK_PREF_OLD = "3rd_app_notification_preference";
    public static final String KEY_NOTIFICATION_MARK_PREF_NEW = "3rd_app_notification_preference_new";
    private static final String KEY_PRE_ARRANGE_PAGE = "arrange_page_preference";
    private static final String KEY_PRE_SHOW_NEW_MARK = "show_new_mark_preference";
    private static final String KEY_PRE_SHOW_CLONABLE_MARK = "show_clonable_mark_preference";
    private static final String KEY_PRE_SHOW_SLIDE_UP_MARK = "show_slide_up_mark_preference";
    private static final int EVENT_ARRANGE_LAYOUT = 1;
    private static final int EVENT_LAYOUT_STYLE = 2;

    private static final String VALUE_PRE_LAYOUT_4x4 = "0";
    private static final String VALUE_PRE_LAYOUT_4x5 = "1";
    private static final String VALUE_PRE_LAYOUT_3x3 = "2";

    /* three ways to show notifications */
    public static int ALL_NOTIFICATION  = 0;
    public static int PART_NOTIFICATION = 1;
    public static int NO_NOTIFICATION   = 2;

    public static final String DB_SHOW_NEW_MARK_ICON = "db_show_new_mark_icon";
    public static final String DB_SHOW_SLIDE_UP_MARK_ICON = "db_show_slide_up_mark_icon";
    public static final String DB_SHOW_CLONABLE_MARK_ICON = "db_show_clonable_mark_icon";
    
    private boolean mIsShowNewMarkIconOldMode;
    private boolean mIsShowSlideUpMarkIconOldMode;
    private boolean mIsShowClonableMarkIconOldMode;

    private IconManager mIconManager;

    /* YUNOS BEGIN */
    // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
    private boolean mIsShowIconMarkChange;
    /* YUNOS END */

    public static final String CONTINUOUS_HOMESHELL_SHOW_ACTION = ContinuousHomeShellReceiver.CONTINUOUS_HOMESHELL_SHOW_ACTION;
    public static final String CONTINUOUS_HOMESHELL_SHOW_KEY = ContinuousHomeShellReceiver.CONTINUOUS_HOMESHELL_SHOW_KEY;
    public static final String KEY_CONTINUOUS_HOMESHELL_STYLE = ContinuousHomeShellReceiver.KEY_CONTINUOUS_HOMESHELL_STYLE;

    public static final String ACTION_ON_MARK_TYPE_CHANGE = "com.aliyun.homeshell.markTypeChange";
    public static final String ACTION_ON_SHOW_NEW_MARK_CHANGE = "com.aliyun.homeshell.showNewMarkChange";
    public static final String ACTION_ON_SHOW_SLIDE_UP_MARK_CHANGE = "com.aliyun.homeshell.showSlideUpMarkChange";
    public static final String ACTION_HOTSEAT_LAYOUT_CHANGE = "com.aliyun.homeshell.HOTSEAT_LAYOUT_CHANGE";
    public static final String ACTION_ON_SHOW_CLONABLE_MARK_CHANGED = "com.aliyun.homeshell.showClonableMarkChanged";

    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    private HWSwitchPreference mFreezePref;
    public static final String FREEZE_HOMESHELL_FLAG = "FREEZE_HOMESHELL_FLAG";
    public static final String AGED_MODE_FLAG = "AGED_MODE_FLAG";
    public static final String KEY_PRENO_AGED_LAYOUT_STYLE = "no_aged_layout_preference";
    /*YUNOS END*/

    public static final String STATIC_HOTSEAT_FLAG = "STATIC_HOTSEAT_FLAG";
    private HWSwitchPreference mStaticPref;

    private HWSwitchPreference mUpdateInfoPref;
    public static final String KEY_PRE_UPDATE_APP_INFO = "update_app_info_preference";
    /*YUNOS BEGIN*/
    //##date:2015/03/23 ##author:xiangyang.ma
    //filter for shortcut in home
    private HWSwitchPreference mShortCutPref;
    public static final String KEY_PRE_SHORTCUT = "shortcut_preference";
    public static final String SHORTCUT_MODE = "shortcut_mode";
    /*YUNOS END*/

    private Context mContext;
    private ActionSheet mLayoutActionSheet;
    private ActionSheet mNotificationActionSheet;
    /* YUNOS BEGIN */
    // ##date:2015-1-23 ##author:zhanggong.zg ##BugID:5732116
    // update card indicator when notification mark preference changed
    private Set<String> mNotificationPackageNames;
    public static final String EXTRA_NOTIFICATION_PACKAGE_NAMES = "com.aliyun.homeshell.NotificationPackageNames";
    /* YUNOS END */
    //bugid: 5258002: receiver to receive theme changed event sent by LauncherModel
    public static String THEME_CHANGED_ACTION = "com.aliyun.homeshellsetting.ACTION_THEME_CHANGED";

    private static final String[] MARK_NOTIFICATION_ITEMS = new String[] {"all", "major", "off"};
    private static final String[] PATTERNS = new String[] {"old", "standard"};
    private static final String[] LOCK_STATUS = new String[] {"on", "off"};
    private static String sDefaultLayoutStyle = "";

    //icon dynamic title color
    private HWSwitchPreference mDyncIconTitleColorPref = null;
    private static final boolean sDyncColorDefault = true;
    private boolean mSuppDyncColor;
    private static final String KEY_ICON_DYNAMIC_TITLE_COLOR = "icon_dynamic_color_preference";

    private void registerThemeChangedReceiver(Context context) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(THEME_CHANGED_ACTION);
        context.registerReceiver(mSettingReceiver, filter);
    }
    private final BroadcastReceiver mSettingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(TAG, "Intent is null, out of memory?");
                return;
            }
            final String action = intent.getAction();
            Log.d(TAG, "action="+action);
            if (THEME_CHANGED_ACTION.equals(action)) {
                updateLayoutPreferenceContent();
            }
        }
    };
    // end of 5258002
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case EVENT_ARRANGE_LAYOUT:
                if (LauncherApplication.mLauncher != null) {
                    LauncherApplication.mLauncher.getWorkspace()
                            .arrangeAllPagesPostDelay(
                                    new HomeShellSetting.Callback() {
                                        @Override
                                        public void onFinish() {
                                            Utils.dismissLoadingDialog();
                                        }
                                    });
                } else {
                    Utils.dismissLoadingDialog();
                }
                break;
            case EVENT_LAYOUT_STYLE:
                LauncherApplication app = (LauncherApplication) mContext
                        .getApplicationContext();
                    app.getModel().changeLayout(msg.arg1, msg.arg2, false, null);
                break;

            default:
                break;
            }
        };
    };
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mIconManager = ((LauncherApplication) this.getApplicationContext()).getIconManager();
        mContext = this;
        sDefaultLayoutStyle = getDefaultLayoutStyleString();
        initActionBar();
        getActionBarView().setTitleColor(getResources().getColor(R.color.setting_title_color));
        setTitle2(this.getResources().getString(R.string.menu_item_homeshellsetting));
        Drawable backIcon= ReflectHelper.Context.getDrawable(this,
                R.drawable.back_icon);
        this.showBackKey(true, backIcon);
        getActionBarView().setBackgroundResource(R.drawable.setting_head);
        ACA.Window.addFlags(this.getWindow(), ACA.Window.FLAG_TRANSLUCENT_STATUS);
        Object systemBarManager = ACA.SystemBarColorManager.newInstance(this);
        if(systemBarManager != null){
            ACA.SystemBarColorManager.setStatusBarColor(systemBarManager, getResources().getColor(R.color.setting_header_color));
            ACA.SystemBarColorManager.setViewFitsSystemWindows(systemBarManager, this, true);
            ACA.SystemBarColorManager.setStatusBarDarkMode(systemBarManager, getWindow(), getResources().getBoolean(R.bool.setting_dark_mode));
        } else {
            ACA.Window.clearFlags(this.getWindow(), ACA.Window.FLAG_TRANSLUCENT_STATUS);
        }

        // getHeaderBuilder().setBackgroundResource(R.drawable.header_bg);
        loadResources();

        mSharedRef = getPreferenceManager().getSharedPreferences();
        mContainer = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(mContainer);

        if (!ConfigManager.isLandOrienSupport()) {
            rebuildLayoutPreference();
        }

        addContinuousHomePreference();
        addNewMarkPreference();
        if (AppCloneManager.isSupportAppClone()) {
            addClonableMarkPreference();
        }

        if (FeatureUtility.supportDyncColor()) {
            addDyncIconTitleColor();
        }

        addNotificationMarkNew();


        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        addFreezePreference();
        /*YUNOS END*/

        /* YUNOS BEGIN */
        //## modules(homeshellsetting) BugId: 7778632
        //## date: 2016/02/19 ## author: xiangnan.xn@alibaba-inc.com
        // AtomRank changes when Launcher create and start,
        // staticHotSeatPreference should not depend on that,
        // use direct condition judgement instead of indirect way
        //## date: 2016/03/15 ## author: wangye.wy
        //## BugID: 7762285: remove atom of xiao yun in hotseat
    /*
        if (!AgedModeUtil.isAgedMode()) {
            addStaticHotSeatPreference();
        }
    */
        /* YUNOS END */

        /*YUNOS BEGIN*/
        //##date:2015/03/23 ##author:xiangyang.ma
        //filter for shortcut in home if ro.yunos.support.shourtcut is yes
        if(FeatureUtility.hasShortCutFeature())
            addShortCutPreference();
        /*YUNOS END*/

        // add ThemeChangeListener when theme changed to update layout Preference
        //ThemeChangedListener.getInstance(this).addListener(this);
        registerThemeChangedReceiver(this);
        addUpdateAppInfoPreference();
        if (LauncherApplication.homeshellSetting != this) {
            LauncherApplication.homeshellSetting = this;
        }
        LauncherModel.homeshellSetting = this;
    }

    private void addUpdateAppInfoPreference() {
        mUpdateInfoPref = new HWSwitchPreference(this);
        mUpdateInfoPref.setTitle(R.string.update_app_info);
        mUpdateInfoPref.setSummary(R.string.update_app_info_summary);
        mUpdateInfoPref.setKey(KEY_PRE_UPDATE_APP_INFO);
        mUpdateInfoPref.setChecked(getUpdateInfoValue(this));
        mContainer.addPreference(mUpdateInfoPref);
        mUpdateInfoPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setUpdateInfoValue(HomeShellSetting.this, (Boolean)newValue);

                Map<String, String> pParams = new HashMap<String, String>();
                pParams.put("status", (Boolean)newValue ? "on" : "off");
                UserTrackerHelper.sendUserReport(HomeShellSetting.this, UserTrackerMessage.MSG_UPDATE_APP_CATEGORY_INFO, pParams);
                return true;
            }
        });
    }

    /*YUNOS BEGIN*/
    //##date:2015/03/23 ##author:xiangyang.ma
    //filter for shortcut in home

    private void addShortCutPreference() {
        mShortCutPref = new HWSwitchPreference(this);
        mShortCutPref.setTitle(R.string.permlab_install_shortcut);
        mShortCutPref.setSummary(R.string.permdesc_install_shortcut);
        mShortCutPref.setKey(KEY_PRE_SHORTCUT);
        mShortCutPref.setChecked(getShortCutValue(this));
        mContainer.addPreference(mShortCutPref);
        mShortCutPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                mShortCutPref.setChecked(isOn);
                setShortCutValue(HomeShellSetting.this,isOn);
                Map<String, String> pParams = new HashMap<String, String>();
                pParams.put("status", isOn ? "on" : "off");
                UserTrackerHelper.sendUserReport(HomeShellSetting.this, UserTrackerMessage.MSG_INSTALL_SHORTCUT, pParams);
                return true;
            }
        });
    }

    private static final String AUTO_UPDATE_APP_INFO = "auto_update_app_info";
    private static boolean getDefaultValue(Context context) {
        return Settings.System.getInt(context.getContentResolver(), AUTO_UPDATE_APP_INFO, 1) == 1;
    }

    public static boolean getUpdateInfoValue(Context context) {
        boolean defaultValue = getDefaultValue(context);
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return defaultValue;
        }
        boolean value = preference.getBoolean(KEY_PRE_UPDATE_APP_INFO, defaultValue);
        return value;
    }

    public static void setUpdateInfoValue(Context context, boolean value) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(KEY_PRE_UPDATE_APP_INFO, value);
        editor.commit();
    }

    public static boolean getShortCutValue(Context context) {
        return (Settings.Secure.getInt(context.getContentResolver(),SHORTCUT_MODE,0) == 1);
    }

    public static void setShortCutValue(Context context, boolean value) {
        Settings.Secure.putInt(context.getContentResolver(),SHORTCUT_MODE,value ? 1 : 0);
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    private void addFreezePreference() {
        // TODO Auto-generated method stub
        mFreezePref = new HWSwitchPreference(this);
        mFreezePref.setTitle(R.string.aged_freeze_homeshell);
        mFreezePref.setSummary(R.string.aged_freeze_homeshell_sub);
        mFreezePref.setKey(FREEZE_HOMESHELL_FLAG);
        mFreezePref.setChecked(getFreezeValue(this));
        mContainer.addPreference(mFreezePref);
        mFreezePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                mFreezePref.setChecked(isOn);
                Map<String, String> param = new HashMap<String, String>();
                param.put(UserTrackerMessage.Key.PATTERN, AgedModeUtil.isAgedMode() ? PATTERNS[0]
                        : PATTERNS[1]);
                param.put(UserTrackerMessage.Key.RESULT, isOn ? LOCK_STATUS[0] : LOCK_STATUS[1]);
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_EDIT_LOCK, param);
                return true;
            }
        });
    }

    private void addStaticHotSeatPreference() {
        // TODO Auto-generated method stub
        mStaticPref = new HWSwitchPreference(this);
        mStaticPref.setTitle(R.string.hotseat_atom);
        mStaticPref.setSummary(R.string.hotseat_atom_summary);
        mStaticPref.setKey(STATIC_HOTSEAT_FLAG);
        mStaticPref.setChecked(getStaticHotSeat(this));
        mContainer.addPreference(mStaticPref);
        mStaticPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                mStaticPref.setChecked(isOn);
                sendBroadcast(new Intent(ACTION_HOTSEAT_LAYOUT_CHANGE));
                return true;
            }
        });
    }

    public static boolean getFreezeValue(Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return false;
        }
        return preference.getBoolean(FREEZE_HOMESHELL_FLAG, false);
    }

    public static void setFreezeValue(Context context, boolean value) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(FREEZE_HOMESHELL_FLAG, value);
        editor.commit();
    }

    public static boolean getStaticHotSeat(Context context) {
        /* YUNOS BEGIN */
        //## modules(Home Shell)
        //## date: 2016/03/15 ## author: wangye.wy
        //## BugID: 7762285: remove atom of xiao yun in hotseat
    /*
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return false;
        }
        boolean defaultValue = ConfigManager.getHotseatAtomRank() > 0 ? true : false;
        if (!preference.contains(STATIC_HOTSEAT_FLAG)) {
            preference.edit().putBoolean(STATIC_HOTSEAT_FLAG, defaultValue).commit();
        }
        return preference.getBoolean(STATIC_HOTSEAT_FLAG, defaultValue);
    */
        return false;
        /* YUNOS END */
    }

    public static void setStaticHotSeat(Context context, boolean value) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(STATIC_HOTSEAT_FLAG, value);
        editor.commit();
    }

    public static void setLayoutValue(Context context, boolean aged) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        String current = preference.getString(KEY_PRE_LAYOUT_STYLE, sDefaultLayoutStyle);

        /* YUNOS BEGIN */
        // ##date:2015/7/24 ##author:zhanggong.zg ##BugID:6214466
        if (aged) {
            // switch to aged mode
            Log.d(TAG, "switch to aged mode");
            if (!TextUtils.isEmpty(current) && !VALUE_PRE_LAYOUT_3x3.equals(current)) {
                // save current normal layout (4x4 or 4x5)
                editor.putString(KEY_PRENO_AGED_LAYOUT_STYLE, current);
                editor.commit();
                Log.d(TAG, "save normal layout: " + current);
            }
            current = VALUE_PRE_LAYOUT_3x3;
        } else {
            // switch to normal mode
            if (!VALUE_PRE_LAYOUT_4x4.equals(current)&&
                !VALUE_PRE_LAYOUT_4x5.equals(current)) {
                // restore previous layout
                Log.d(TAG, "switch to normal mode");
                String backup = preference.getString(KEY_PRENO_AGED_LAYOUT_STYLE, "");
                Log.d(TAG, "previous layout: " + backup);
                //if (VALUE_PRE_LAYOUT_4x4.equals(backup)) {
                    current = VALUE_PRE_LAYOUT_4x4;
                //} else {
                //    current = VALUE_PRE_LAYOUT_4x5;
                //}
            }
        }
        editor.putString(KEY_PRE_LAYOUT_STYLE, current);
        editor.commit();
        /* YUNOS END */
    }
    /*YUNOS END*/

    /* YUNOS BEGIN */
    // ##date:2015/9/9 ##author:zhanggong.zg ##BugID:6398960
    public static boolean isValidLayout(int countX, int countY, boolean agedMode) {
        if (ConfigManager.isLandOrienSupport()) {
            return true;
        }
        if (agedMode) {
            return countX == 3 && countY == 3;
        } else {
            return countX == 4 && (countY == 4 || countY == 5);
        }
    }
    /* YUNOS END */

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /* YUNOS BEGIN */
        // ##date:2014/12/3 ##author:zhanggong.zg ##BugID:5157204
        // Updates layout preference visibility
        rebuildLayoutPreference();
        /* YUNOS END */
    }

    /* YUNOS BEGIN */
    // ##date:2015/9/9 ##author:zhanggong.zg ##BugID:6398960
    private void rebuildLayoutPreference(){
        // create layout preference if necessary
        if (mLayoutPref == null) {
            mLayoutPref = new Preference(this);
            mLayoutPref.setTitle(R.string.settings_icon_layout);
            mLayoutPref.setKey(KEY_PRE_LAYOUT_STYLE);
            mLayoutPref.setOnPreferenceChangeListener(this);
            mLayoutPref.setOnPreferenceClickListener(this);
        }
        if (!AgedModeUtil.isAgedMode()) {
            mLayoutPref.setEnabled(true);
            // add preference to category
            mContainer.addPreference(mLayoutPref);
            // update summary
            String layoutvalue = mSharedRef.getString(KEY_PRE_LAYOUT_STYLE, sDefaultLayoutStyle);
            if (!VALUE_PRE_LAYOUT_4x4.equals(layoutvalue) &&
                !VALUE_PRE_LAYOUT_4x5.equals(layoutvalue)) {
                setLayoutValue(mContext, false);
            }
            mOldLayoutMode = getCurrentLayoutIndex();
            updateLayoutSummary();
            mContainer.removePreference(mLayoutPref);
        } else {
            // hide preference when in aged mode or big card theme
            if (mContainer.findPreference(KEY_PRE_LAYOUT_STYLE) != null) {
                mContainer.removePreference(mLayoutPref);
            }
            /*
                Before bug-5157204, this preference item was only disabled (but not removed)
                when currently in aged mode or big card theme, and a summary was shown:
                    mLayoutPref.setEnabled(false);
                    mLayoutPref.setSummary(R.string.forbid_layout_change_when_3_3) or
                    mLayoutPref.setSummary(R.string.forbid_layout_change_when_big)
             */
        }
    }
    /*YUNOS END*/

    private void addNewMarkPreference(){

        mShowNewMarkIconPref = new HWSwitchPreference(this);
//        mShowNewMarkIconPref.setSummaryOff(R.string.setttings_mark_not_enabled);
//        mShowNewMarkIconPref.setSummaryOn(R.string.setttings_mark_enabled);
        mShowNewMarkIconPref.setTitle(R.string.settings_icon_mark);
        /* YUNOS BEGIN */
        // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
        mShowNewMarkIconPref.setKey(KEY_PRE_SHOW_NEW_MARK);
        /* YUNOS END */
        mContainer.addPreference(mShowNewMarkIconPref);
        mIsShowNewMarkIconOldMode = LauncherModel.isShowNewMarkIcon();
        mShowNewMarkIconPref.setChecked(mIsShowNewMarkIconOldMode);
        mShowNewMarkIconPref.setOnPreferenceChangeListener(this);
        /* YUNOS BEGIN */
        // ##date:2014/07/04 ##author:hongchao.ghc ##BugID:130855
        UserTrackerHelper.bindPageName(this, UserTrackerMessage.MSG_LAUNCHER_SETTING);
        /* YUNOS END */
        mShowSlideUpMarkIconPref = new HWSwitchPreference(this);
        mShowSlideUpMarkIconPref.setTitle(R.string.settings_icon_slide_up_mark);
        mShowSlideUpMarkIconPref.setKey(KEY_PRE_SHOW_SLIDE_UP_MARK);
        mContainer.addPreference(mShowSlideUpMarkIconPref);
        mIsShowSlideUpMarkIconOldMode = LauncherModel.isShowSlideUpMarkIcon();
        mShowSlideUpMarkIconPref.setChecked(mIsShowSlideUpMarkIconOldMode);
        mShowSlideUpMarkIconPref.setOnPreferenceChangeListener(this);
    }

    private void addClonableMarkPreference() {
      mShowClonableMarkIconPref = new HWSwitchPreference(this);
      mShowClonableMarkIconPref.setTitle(R.string.settings_clonable_mark);
      mShowClonableMarkIconPref.setKey(KEY_PRE_SHOW_CLONABLE_MARK);
      mContainer.addPreference(mShowClonableMarkIconPref);
      mIsShowClonableMarkIconOldMode = LauncherModel.isShowClonableMarkIcon();
      mShowClonableMarkIconPref.setChecked(mIsShowClonableMarkIconOldMode);
      mShowClonableMarkIconPref.setOnPreferenceChangeListener(this);
    }

    private void addNotificationMarkNew(){
        if( FeatureUtility.isYunOS2_9System() ) return;
        int notificationType = LauncherModel.getNotificationMarkType();
        m3rdAppNotificationPrefNew = new HWSwitchPreference(this);
        m3rdAppNotificationPrefNew.setTitle(R.string.settings_notification_mark_title);
        m3rdAppNotificationPrefNew.setKey(KEY_NOTIFICATION_MARK_PREF_NEW);
        m3rdAppNotificationPrefNew.setChecked(notificationType != NO_NOTIFICATION);
        mContainer.addPreference(m3rdAppNotificationPrefNew);
        m3rdAppNotificationPrefNew.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                int index = isOn ? ALL_NOTIFICATION : NO_NOTIFICATION;
                m3rdAppNotificationPrefNew.setChecked(isOn);
                // user tracker
                Map<String, String> param = new HashMap<String, String>();
                param.put(UserTrackerMessage.Key.RESULT, MARK_NOTIFICATION_ITEMS[index]);
                UserTrackerHelper.sendUserReport(UserTrackerMessage.MSG_MARK_NOTIFICATION_SELECT, param);
                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);
                sp.edit().putInt(KEY_NOTIFICATION_MARK_PREF_NEW, index).commit();
                // update homeshell
                Intent intent = new Intent(ACTION_ON_MARK_TYPE_CHANGE);
                intent.putExtra("showType", index);
                sendBroadcast(intent);
                Log.d(TAG,"startNotificationDialog onclick");
                return true;
            }
        });
    }

    private void loadResources() {
        mLayoutTitle = getResources().getStringArray(R.array.entries_layout_preference);
        mLayoutValue = getResources().getStringArray(R.array.entryvalues_layout_preference);
    }

    private void addContinuousHomePreference() {
        mContinuousHomePref = new HWSwitchPreference(this);
        mContinuousHomePref.setTitle(R.string.settings_continuous_homeshell);
        mContinuousHomePref.setKey(KEY_CONTINUOUS_HOMESHELL_STYLE);
        mContainer.addPreference(mContinuousHomePref);

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean isOn = true;
        if (preference.contains(KEY_CONTINUOUS_HOMESHELL_STYLE)) {
            isOn = preference.getBoolean(KEY_CONTINUOUS_HOMESHELL_STYLE, true);
        } else {
            SharedPreferences.Editor preEditor = preference.edit();
            preEditor.putBoolean(KEY_CONTINUOUS_HOMESHELL_STYLE, true);
            preEditor.commit();
        }
        mContinuousHomePref.setChecked(isOn);

        mContinuousHomePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                /* YUNOS BEGIN */
                // ##date:2014/07/28 ##author:hongchao.ghc ##BugID:142221
                // add launcher setting looping Buried
                Map<String, String> pParams = new HashMap<String, String>();
                pParams.put("status", isOn ? "on" : "off");
                UserTrackerHelper.sendUserReport(HomeShellSetting.this, UserTrackerMessage.MSG_LAUNCHER_SETTING_LOOPING, pParams);
                /* YUNOS END */
                mContinuousHomePref.setChecked(isOn);
                Intent mHomeLoopIntent = new Intent(CONTINUOUS_HOMESHELL_SHOW_ACTION);
                mHomeLoopIntent.putExtra(CONTINUOUS_HOMESHELL_SHOW_KEY, isOn);
                sendBroadcast(mHomeLoopIntent);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        /* YUNOS BEGIN */
        // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:130626
        // Buried Point Optimization
        UserTrackerHelper.pageEnter(UserTrackerMessage.MSG_LAUNCHER_SETTING);
        /* YUNOS END */
        mIsShowNewMarkIconOldMode = LauncherModel.isShowNewMarkIcon();
        mShowNewMarkIconPref.setChecked(mIsShowNewMarkIconOldMode);
        mIsShowSlideUpMarkIconOldMode = LauncherModel.isShowSlideUpMarkIcon();
        mShowSlideUpMarkIconPref.setChecked(mIsShowSlideUpMarkIconOldMode);
        mIsShowClonableMarkIconOldMode = LauncherModel.isShowClonableMarkIcon();
        if (AppCloneManager.isSupportAppClone()) {
            mShowClonableMarkIconPref.setChecked(mIsShowClonableMarkIconOldMode);
        }
        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (mFreezePref != null) {
            mFreezePref.setChecked(getFreezeValue(this));
        }
        /* YUNOS END */
        if (mDyncIconTitleColorPref != null) {
            mDyncIconTitleColorPref.setChecked(getIconDyncColor(this));
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        /* YUNOS BEGIN */
        // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:130626
        // Buried Point Optimization
        UserTrackerHelper.pageLeave(UserTrackerMessage.MSG_LAUNCHER_SETTING);
        /* YUNOS END */
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        notifyChange();

        // BugID:5566012:close actionsheet when leaving settings(wenliang.dwl)
        if( mLayoutActionSheet != null ) mLayoutActionSheet.dismiss(false);
        if( mNotificationActionSheet != null ) mNotificationActionSheet.dismiss(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(EVENT_ARRANGE_LAYOUT);
        mHandler.removeMessages(EVENT_LAYOUT_STYLE);
        //ThemeChangedListener.getInstance(this).removeListener(this);
        unregisterReceiver(mSettingReceiver);
        LauncherApplication.homeshellSetting = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        /* YUNOS BEGIN */
        // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:130626
        // Buried Point Optimization
        if (KEY_PRE_SHOW_NEW_MARK.equals(key)) {
            boolean value = (Boolean) objValue;
            updateShowMarkIconSummary(value);
            /* YUNOS BEGIN */
            // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:5126243
            // add mark new buried point
            Map<String, String> pParams = new HashMap<String, String>();
            pParams.put("status", value ? "on" : "off");
            UserTrackerHelper.sendUserReport(HomeShellSetting.this,
                    UserTrackerMessage.MSG_LAUNCHER_SETTING_MARK_NEW, pParams);
            /* YUNOS END */

            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged model
        } else if (KEY_PRE_SHOW_SLIDE_UP_MARK.equals(key)) {
            boolean value = (Boolean) objValue;
            updateShowSlideUpMarkIconSummary(value);
        } else if (KEY_PRE_LAYOUT_STYLE.equals(key)) {
            String currentString = (String) objValue;
            for (int i = 0; i < mLayoutValue.length; i++) {
                if (mLayoutValue[i].equals(currentString)) {
                    mLayoutPref.setSummary(mLayoutTitle[i]);
                }
            }
            /* YUNOS END */
        } else if (KEY_PRE_SHOW_CLONABLE_MARK.equals(key)) {
            boolean value = (Boolean) objValue;
            updateShowClonableMarkSummary(value);
        }
        /* YUNOS END */
        return true;
    }

    private void notifyChange() {
        /* YUNOS BEGIN */
        // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
        boolean isShowIconNewMode = mShowNewMarkIconPref.isChecked();
        mIsShowIconMarkChange = mIsShowNewMarkIconOldMode ^ isShowIconNewMode;
        boolean isNotificationPrefChange = mNotificationPackageNames != null;
        boolean isShowSlideUpMarkChange = mIsShowSlideUpMarkIconOldMode ^
                mShowSlideUpMarkIconPref.isChecked();
        boolean isShowClonableMarkChange = mIsShowClonableMarkIconOldMode
                ^ (AppCloneManager.isSupportAppClone() && mShowClonableMarkIconPref.isChecked());
        if (isShowSlideUpMarkChange) {
            Intent intent = new Intent();
            intent.setAction(LauncherModel.ACTION_UPDATE_SLIDEUP);
            sendBroadcast(intent);
        }
        if (mIsShowIconMarkChange || isNotificationPrefChange) {
            Intent intent = new Intent();
            intent.setAction(LauncherModel.ACTION_UPDATE_LAYOUT);
            /* YUNOS BEGIN */
            // ##date:2015-1-23 ##author:zhanggong.zg ##BugID:5732116
            // update card indicator when notification mark preference changed
            if (isNotificationPrefChange) {
                intent.putExtra(EXTRA_NOTIFICATION_PACKAGE_NAMES,
                        mNotificationPackageNames.toArray(new String[0]));
                mNotificationPackageNames = null;
            }
            /* YUNOS END */
            sendBroadcast(intent);
        }
        if (isShowClonableMarkChange && AppCloneManager.isSupportAppClone()) {
            Intent intent = new Intent();
            intent.setAction(LauncherModel.ACTION_UPDATE_CLONABLE);
            sendBroadcast(intent);
        }
        /* YUNOS END */
    }

    private void updateLayoutSummary() {
        String currentValue = mSharedRef.getString(KEY_PRE_LAYOUT_STYLE, sDefaultLayoutStyle);
        for (int i = 0; i < mLayoutValue.length; i++) {
            if (currentValue.equals(mLayoutValue[i])) {
                mLayoutPref.setSummary(mLayoutTitle[i]);
            }
        }
    }

    private void updateShowSlideUpMarkIconSummary(boolean isShow) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DB_SHOW_SLIDE_UP_MARK_ICON, isShow);
        editor.commit();
        sendBroadcast(new Intent(ACTION_ON_SHOW_SLIDE_UP_MARK_CHANGE));
    }

    private void updateShowMarkIconSummary(boolean isShow) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DB_SHOW_NEW_MARK_ICON, isShow);
        editor.commit();
        sendBroadcast(new Intent(ACTION_ON_SHOW_NEW_MARK_CHANGE));
    }

    private void updateShowClonableMarkSummary(boolean isShow) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DB_SHOW_CLONABLE_MARK_ICON, isShow);
        editor.commit();
        sendBroadcast(new Intent(ACTION_ON_SHOW_CLONABLE_MARK_CHANGED));
    }

    public static int getSlideEffectMode(Context ctx) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (preference == null) {
            return 0;
        }
        String value = preference.getString(KEY_PRE_EFFECT_STYLE, "0");
        int mode = Integer.parseInt(value);
        return mode;
    }
    public static boolean getLoopDesktopMode(Context ctx){
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(ctx) ;
        if(preference == null){
            return false;
        }
        return preference.getBoolean("loop_desktop_preference",false);
    }
    private boolean mNeedSavePrevLayout;
    private int mLastLayout;
    public void checkNeedLayoutChange(int x, int y) {
        if (!mNeedSavePrevLayout) return;
        mNeedSavePrevLayout = false;
        int countX = 0;
        int countY = 0;
        switch (mLastLayout) {
        case 0:
            countX = 4;
            countY = 4;
            break;
        case 1:
            countX = 4;
            countY = 5;
            break;
        case 2:
            countX = 3;
            countY = 3;
            break;
        default:
            countX = 4;
            countY = 4;
        }

        if (LauncherModel.checkGridSize(countX, countY) && (x!=countX || y!=countY)) {
            mSharedRef.edit().putString(KEY_PRE_LAYOUT_STYLE, mLayoutValue[mLastLayout]).commit();
            Message msg = new Message();
            msg.what = EVENT_LAYOUT_STYLE;
            msg.arg1 = countX;
            msg.arg2 = countY;
            mHandler.removeMessages(EVENT_LAYOUT_STYLE);
            mHandler.sendMessageDelayed(msg, 200);
        }
    }

    private void notifyLayoutChanged() {
        int mode = getCurrentLayoutIndex();
        if (mode == mOldLayoutMode) {
            return;
        }
        mOldLayoutMode = mode;
        int countX = 0;
        int countY = 0;
        switch (mode) {
        case 0:
            countX = 4;
            countY = 4;
            break;
        case 1:
            countX = 4;
            countY = 5;
            break;
        /* YUNOS BEGIN */
        // ##date:2014/10/16 ##author:yangshan.ys##5157204
        // for 3*3 layout
        case 2:
            countX = 3;
            countY = 3;
            break;
        default:
            countX = 4;
            countY = 4;
        }
        /* YUNSO END */

        if (LauncherModel.checkGridSize(countX, countY)) {
            Utils.showLoadingDialog(HomeShellSetting.this);
            Message msg = new Message();
            msg.what = EVENT_LAYOUT_STYLE;
            msg.arg1 = countX;
            msg.arg2 = countY;
            mHandler.removeMessages(EVENT_LAYOUT_STYLE);
            mHandler.sendMessageDelayed(msg, 200);
        }
    }

    private String getLayoutValue( int countX, int countY){
        String layoutTitle = ""+countX+"x"+countY;
        for( int i = 0; i < mLayoutTitle.length; i++ ){
            if( layoutTitle.equals(mLayoutTitle[i])){
                return mLayoutValue[i];
            }
        }
        return "0";
    }

    private int getCurrentLayoutIndex() {
        String currentString = mSharedRef.getString(KEY_PRE_LAYOUT_STYLE, sDefaultLayoutStyle);
        for (int i = 0; i < mLayoutValue.length; i++) {
            if (mLayoutValue[i].equals(currentString)) {
                return i;
            }
        }
        return 0;
    }

    private void startLayoutDialog() {
        if (mLayoutActionSheet != null && mLayoutActionSheet.isShowing()) {
            return;
        }
        mLayoutActionSheet = new ActionSheet(this);
        mLayoutActionSheet.setSingleChoiceItems(mLayoutTitle, getCurrentLayoutIndex(), new SingleChoiceListener() {

            @Override
            public void onDismiss(ActionSheet actionSheet) {
            }

            @Override
            public void onClick(int position) {
                /* YUNOS BEGIN */
                // ##date:2014/10/16 ##author:yangshan.ys##5157204
                // for 3*3 layout

                UserTrackerHelper.sendLauncherLayoutResult(Integer.parseInt(mLayoutValue[position]) + 1);
                UserTrackerHelper
                    .pageLeave(UserTrackerMessage.MSG_LAUNCHER_SETTING_LAUNCHER_SETTING_LAYOUT);
                LauncherApplication.getLauncher();
                if (AgedModeUtil.isAgedMode() == Launcher.mIsAgedMode) {
                    mSharedRef.edit().putString(KEY_PRE_LAYOUT_STYLE, mLayoutValue[position]).commit();
                    updateLayoutSummary();
                    notifyLayoutChanged();
                } else {
                    mLayoutPref.setSummary(mLayoutTitle[position]);
                    mNeedSavePrevLayout = true;
                    mLastLayout = position;
                }
                /* YUNSO END */
            }
        });
        mLayoutActionSheet.setDispalyStyle(ActionSheet.DisplayStyle.PHONE);
        mLayoutActionSheet.show(getWindow().getDecorView());
        setDialogDarkMode(mLayoutActionSheet.showWithDialog2().getWindow());
    }

    private void setDialogDarkMode(Window win) {
        Object systemBarManager = ACA.SystemBarColorManager.newInstance(win);
        ACA.SystemBarColorManager.setStatusBarDarkMode(systemBarManager, win, true);
    }
    @Override
    public boolean onPreferenceClick(Preference preference) {
        final String key = preference.getKey();
        /* YUNOS BEGIN */
        // ##date:2014/07/04 ##author:hongchao.ghc ##BugID:130855
        if (KEY_PRE_LAYOUT_STYLE.equals(key)) {
            UserTrackerHelper.sendUserReport(HomeShellSetting.this,
                    UserTrackerMessage.MSG_LAUNCHER_SETTING_LAYOUT);
            UserTrackerHelper
                    .pageEnter(UserTrackerMessage.MSG_LAUNCHER_SETTING_LAUNCHER_SETTING_LAYOUT);
            startLayoutDialog();
        } else if (KEY_PRE_ARRANGE_PAGE.equals(key)) {
            UserTrackerHelper.sendUserReport(HomeShellSetting.this,
                    UserTrackerMessage.MSG_LAUNCHER_SETTING_ARRANGE);
            /* YUNOS BEGIN */
            // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
            Utils.showLoadingDialog(HomeShellSetting.this);
            Dialog dialog = Utils.getLoadingDialog(HomeShellSetting.this);
            if (dialog != null) {
                setDialogDarkMode(dialog.getWindow());
            }
            mHandler.removeMessages(EVENT_ARRANGE_LAYOUT);
            mHandler.sendEmptyMessageDelayed(EVENT_ARRANGE_LAYOUT, 500);
            /* YUNOS END */
        }
        /* YUNOS END */

        return false;
    }

    /* YUNOS BEGIN */
    // ##date:2014/08/12 ##author:hongchao.ghc ##BugID:146637
    public interface Callback
    {
        public void onFinish();
    }
    /* YUNOS END */

    /**
     * update Layout Preference when theme changed
     */
    private void updateLayoutPreferenceContent(){
        boolean isSupportCard = mIconManager.supprtCardIcon();
        boolean isEnabled = mLayoutPref.isEnabled();
        Log.e(TAG, "isSupportCard=" + isSupportCard + ",isEnabled=" + isEnabled);
        if (AgedModeUtil.isAgedMode()) {
            mLayoutPref.setEnabled(false);
            mLayoutPref.setSummary(R.string.forbid_layout_change_when_3_3);
        } else {
                mLayoutPref.setEnabled(true);
                mLayoutPref.setOnPreferenceChangeListener(this);
                mLayoutPref.setOnPreferenceClickListener(this);
                int countY = LauncherModel.getCellCountY();
                String layoutvalue = getLayoutValue(4, countY);
                mSharedRef.edit().putString(KEY_PRE_LAYOUT_STYLE, layoutvalue).commit();
                mOldLayoutMode = getCurrentLayoutIndex();
                updateLayoutSummary();
        }
    }

    public void updateLayoutPreference() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateLayoutPreferenceContent();
            }});
    }

    private String getDefaultLayoutStyleString() {
        int cellCountX = mContext.getResources().getInteger(R.integer.cell_count_x);
        int cellCountY = mContext.getResources().getInteger(R.integer.cell_count_y);
        if (cellCountX == 4 && cellCountY == 4) {
            return VALUE_PRE_LAYOUT_4x4;
        } else if (cellCountX == 3 && cellCountY == 3) {
            return VALUE_PRE_LAYOUT_3x3;
        /*} else if (cellCountX == 4 && cellCountY == 5) {
            return VALUE_PRE_LAYOUT_4x5;*/
        } else {
            return VALUE_PRE_LAYOUT_4x4;
        }
    }

    private void addDyncIconTitleColor() {
        mDyncIconTitleColorPref = new HWSwitchPreference(this);
        mDyncIconTitleColorPref.setTitle(R.string.setting_icon_dynamic_title_color);
        mDyncIconTitleColorPref.setKey(KEY_ICON_DYNAMIC_TITLE_COLOR);
        mContainer.addPreference(mDyncIconTitleColorPref);
        mSuppDyncColor = getIconDyncColor(this);
        mDyncIconTitleColorPref.setChecked(mSuppDyncColor);
        mDyncIconTitleColorPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isOn = (Boolean) newValue;
                mDyncIconTitleColorPref.setChecked(isOn);
                updateIconColor(isOn);
                setIconDyncColor(HomeShellSetting.this, isOn);
                return true;
            }
        });
    }

    public static boolean getIconDyncColor(Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return false;
        }
        return preference.getBoolean(KEY_ICON_DYNAMIC_TITLE_COLOR, sDyncColorDefault);
    }

    private void setIconDyncColor(Context context, boolean value) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        if (preference == null) {
            return;
        }
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean(KEY_ICON_DYNAMIC_TITLE_COLOR, value);
        editor.commit();
    }

    private void updateIconColor(boolean update) {
       if (mSuppDyncColor == update) {
           return;
       }
       mSuppDyncColor = update;
       if (update && LauncherApplication.getLauncher() != null) {
           LauncherApplication.getLauncher().getTitleColorManager().needUpdateColor();
       }
    }
}
