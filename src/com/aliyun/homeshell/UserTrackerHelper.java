package com.aliyun.homeshell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;
import com.aliyun.ams.ta.Tracker;
import com.aliyun.ams.ta.utils.MapUtils;
import com.aliyun.ams.ta.utils.StringUtils;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.utility.FeatureUtility;

public class UserTrackerHelper {

    private static final boolean DEBUG = false;
    private static final String TAG = "UserTrackerHelper";

    private static final String LABEL_SCREEN_STATUS = "SCREEN_STATUS";
    private static final String LABEL_FOLDER_STATUS = "FOLDER_STATUS";
    private static final String LABEL_ICON_STATUS = "ICON_STATUS";
    private static final String LABEL_DAY_APP = "DAY";
    private static final int EVENT_SMART_GESTURE = 19999;
    /* YUNOS BEGIN */
    // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:131880
    // buried synchronization code
    /* YUNOS BEGIN */
    // ##date:2014/06/25 ##author:hongchao.ghc ##BugID:132401
    // crash exception null pointer
    private static Tracker mTracker;
    /* YUNOS BEGIN */
    // ##date:2014/07/04 ##author:hongchao.ghc ##BugID:130855
    private static Map<Class<? extends Activity>, String> mPageNameCache = new HashMap<Class<? extends Activity>, String>();

    public static void bindPageName(Activity activity, String pageName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (mPageNameCache == null) {
            mPageNameCache = new HashMap<Class<? extends Activity>, String>();
        }
        if (activity != null) {
            mPageNameCache.put(activity.getClass(), pageName);
        }
        if (mTracker != null) {
            mTracker.bindPageName(mPageNameCache);
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/09/03 ##author:jiangjun ##Add bindPageName for global seach
    //BEGIN-TA
    public static void bindPageName(Class<? extends Activity> activityClass, String pageName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (mPageNameCache == null) {
            mPageNameCache = new HashMap<Class<? extends Activity>, String>();
        }
        if (activityClass != null) {
            mPageNameCache.put(activityClass, pageName);
        }
        if (mTracker != null) {
            mTracker.bindPageName(mPageNameCache);
        }
    }

    public static void sendUserReport(Class<? extends Activity> activityClass, String controlName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(activityClass, controlName);
        }
    }

    public static void sendUserReport(Class<? extends Activity> activityClass, String controlName,
            Map<String, String> pParams) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(activityClass, controlName, pParams);
        }
    }

    public static void commitEvent(String eventName, Map<String, String> pParams) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "commitEvent, eventName =" + eventName);
        }
        if (mTracker != null) {
            mTracker.commitEvent(eventName, pParams);
        }
    }
    //END-TA
    /* YUNOS END */

    public static void sendUserReport(Activity activity, String controlName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(activity.getClass(), controlName);
        }
    }

    /* YUNOS END */
    /* YUNOS BEGIN */
    // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:5126243
    // add mark new buried point
    public static void sendUserReport(Activity activity, String controlName,
            Map<String, String> pParams) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(activity.getClass(), controlName, pParams);
        }
    }
    /* YUNOS END */
    public static void sendUserReport(String controlName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(Launcher.class, controlName);
        }
    }

    public static void sendUserReport(String controlName, String param) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName + ",param=" + param);
        }

        Map<String, String> map = null;
        if (param != null) {
            map = StringUtils.convertStringAToMap(param);
        }
        if (mTracker != null) {
            mTracker.ctrlClicked(Launcher.class, controlName, map);
        }

    }

    public static void sendUserReport(String controlName, Map<String, String> param) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "sendUserEvent, controlName=" + controlName + ",param=" + param);
        }

        if (mTracker != null) {
            mTracker.ctrlClicked(Launcher.class, controlName, param);
        }

    }

    public static void init(Context context) {
        if (DEBUG) {
            Log.d(TAG, "init");
        }

        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }

        StatConfig.getInstance().setContext(context);
        StatConfig.getInstance().turnOnDebug();
        mTracker = TA.getInstance().getTracker("21710403");
        TA.getInstance().setDefaultTracker(mTracker);

    }

    public static void deinit() {
        if (DEBUG) {
            Log.d(TAG, "deinit");
        }
    }

    public static void pageEnter(Activity pActivity) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "pageEnter, name=" + pActivity);
        }
        if (mTracker != null) {
            mTracker.activityStart(pActivity);
        }
    }

    public static void pageLeave(Activity pActivity) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "pageLeave, name=" + pActivity);
        }
        if (mTracker != null) {
            mTracker.activityStop(pActivity);
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:130626
    // Buried Point Optimization
    public static void pageEnter(String pPageName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "pageEnter, pPageName=" + pPageName);
        }
        if (mTracker != null) {
            mTracker.pageEnter(pPageName);
        }
    }

    public static void pageLeave(String pPageName) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "pageLeave, pPageName=" + pPageName);
        }
        if (mTracker != null) {
            mTracker.pageLeave(pPageName);
        }
    }
    /* YUNOS END */

    public static void folderStatus(List<FolderInfo> list) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "folderStatus:");
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("count", (list == null ? 0 : list.size()) + "");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(LABEL_FOLDER_STATUS, map);
        }
    }

    public static void iconStatus(List<ItemInfo> list) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "iconStatus:");
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("count", (list == null ? 0 : list.size()) + "");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(LABEL_ICON_STATUS, map);
        }
    }

    public static void screenStatus(int screens) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "screenStatus:" + screens);
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("count", screens + "");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(LABEL_SCREEN_STATUS, map);
        }
    }

    //BugID:5717551:userTrack for widget and shortcut count
    public static void wigdetStatus(int count, boolean hasAccelerateShortcut, boolean hasLockShortcut) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "wigdetStatus:");
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("status", count + "");
        lProperties.setProperty("accelerate", hasAccelerateShortcut ? "exist" : "non");
        lProperties.setProperty("lock_screen", hasLockShortcut ? "exist" : "non");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(UserTrackerMessage.MSG_WIDGET_STATUS, map);
        }
    }

    //BugID:5717551:userTrack for widget and shortcut count
    public static void shortcutStatus(int count) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "shortcutStatus:");
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("status", count + "");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(UserTrackerMessage.MSG_SHORTCUT_STATUS, map);
        }
    }

    public static void continueStatus(boolean status) {
        if (DEBUG) {
            Log.d(TAG, "continueStatus:" + status);
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("status", status ? "on" : "off");
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(UserTrackerMessage.MSG_LOOP_LAUNCHER_RESULTS, map);
        }
    }

    public static void dayAppStatus(int dayCount, String string) {
        // TODO Auto-generated method stub
        if (DEBUG) {
            Log.d(TAG, "dayAppStatus:" + dayCount + "," + string);
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("pkgName", string);
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(LABEL_DAY_APP + (dayCount + 1), map);
        }
    }

    /* YUNOS BEGIN */
    // ##date:2014/06/23 ##author:hongchao.ghc ##BugID:130626
    // Buried Point Optimization
    public static void sendLauncherEffectsResult(int position) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Effects position:" + position);
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("position", String.valueOf(position));
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(UserTrackerMessage.MSG_LAUNCHER_SETTING_EFFECTS_RESULT, map);
        }
    }

    public static void sendLauncherLayoutResult(int position) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Layout position:" + position);
        }
        Properties lProperties = new Properties();
        lProperties.setProperty("position", String.valueOf(position));
        Map<String, String> map = MapUtils.convertPropertiesToMap(lProperties);
        if (mTracker != null) {
            mTracker.commitEvent(UserTrackerMessage.MSG_LAUNCHER_SETTING_LAYOUT_RESULT, map);
        }
    }

    /* YUNOS END */
    public static void entryPageBegin(String pageLabel) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "entryLauncherBegin:" + pageLabel);
        }
        if (mTracker != null) {
            mTracker.commitEventBegin(pageLabel, null);
        }
    }

    public static void entryPageEnd(String pageLabel) {
        if(Build.YUNOS_CTA_SUPPORT) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "entryLauncherEnd:" + pageLabel);
        }
        if (mTracker != null) {
            mTracker.commitEventEnd(pageLabel, null);
        }
    }
    /* YUNOS END */

    /*BugID:5894707:modify drag icon user tracker*/
    public static void sendDragIconReport(ItemInfo item, long origcontainer, long container, int screen, int x, int y) {
        if (item == null) {
            return;
        }
        final int itemType = item.itemType;
        String tmptitle = null;
        final long oldcontainer = origcontainer;
        final long newcontainer = container;
        final int oldscreen = item.screen;
        final int newscreen = screen;
        final int oldx = item.cellX;
        final int newx = x;
        final int oldy = item.cellY;
        final int newy = y;
        Intent tmpintent = null;
        if (item.title != null) {
            tmptitle = item.title.toString();
        }
        if (item instanceof ShortcutInfo && ((ShortcutInfo) item).intent != null) {
            tmpintent = new Intent(((ShortcutInfo) item).intent);
        } else if (item instanceof LauncherAppWidgetInfo) {
            ComponentName cmpname = ((LauncherAppWidgetInfo)item).providerName;
            if (cmpname != null) {
                tmptitle = cmpname.getPackageName();
            }
        }
        final String title = tmptitle;
        final Intent finalintent = tmpintent;
        Runnable sendReport = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "liuhao usertrack thread in");
                Map<String, String> param = new HashMap<String, String>();
                boolean status = true;
                switch (itemType) {
                case Favorites.ITEM_TYPE_APPLICATION:
                    param.put("itemtype", "app");
                    if (finalintent != null && finalintent.getComponent() != null) {
                        param.put("pkgName", finalintent.getComponent().getPackageName());
                    } else {
                        status = false;
                    }
                    break;
                case Favorites.ITEM_TYPE_SHORTCUT:
                    param.put("itemtype", "shortcut");
                    param.put("shortcutName", title);
                    break;
                case Favorites.ITEM_TYPE_FOLDER:
                    param.put("itemtype", "folder");
                    param.put("FolderName", title);
                    break;
                case Favorites.ITEM_TYPE_SHORTCUT_DOWNLOADING:
                    param.put("itemtype", "downloading");
                    String name = null;
                    if (finalintent != null) {
                        name = finalintent.getStringExtra(AppDownloadManager.TYPE_PACKAGENAME);
                    }
                    if (name == null) {
                        status = false;
                    } else {
                        param.put("downloadPkgName", name);
                    }
                    break;
                case Favorites.ITEM_TYPE_APPWIDGET:
                    param.put("itemtype", "appwidget");
                    param.put("WigdetName", title);
                    break;
                case Favorites.ITEM_TYPE_GADGET:
                    param.put("itemtype", "gadget");
                    param.put("GadgetName", title);
                    break;
                default:
                    status = false;
                    Log.d(TAG, "the itemtype error");
                }

                if (status == false) {
                    return;
                }
                StringBuilder oldposition = new StringBuilder();
                oldposition.append(" container " + oldcontainer
                                        + " screen " + oldscreen
                                        + " cellX "  + oldx
                                        + " cellY " + oldy);
                param.put("From", oldposition.toString());
                StringBuilder newposition = new StringBuilder();
                newposition.append(" container " + newcontainer
                                        + " screen " + newscreen
                                        + " cellX "  + newx
                                        + " cellY " + newy);
                param.put("To", newposition.toString());

                sendUserReport(UserTrackerMessage.MSG_DRAG_ICON, param);
            }
        };
        LauncherApplication.getLauncher().getHandler().post(sendReport);
    }

    public static void sendCustomEvent(String eventId, Map<String, String> param) {
        if (DEBUG) {
            Log.d(TAG, "sendCustomEvent:id=" + eventId + ",param=" + param);
        }
        if (mTracker != null) {
            mTracker.commitEvent(eventId, param);
        }
    }
    public static void reportSmartSensorEvent() {
        if (mTracker != null) {
            mTracker.commitEvent("",
                    EVENT_SMART_GESTURE, "smart_changepic", null, null, null);
        }
    }
}
