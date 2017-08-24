package com.aliyun.homeshell.smartlocate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherProvider;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.model.LauncherModel.ItemVisitor;
import com.aliyun.utility.utils.ACA;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

public final class SuggestedApps {

    private static final String TAG = "SuggestedApps";

    private static final int MAX_NUM_OF_NEW_APPS = 8;
    private static final int MAX_NUM_OF_RECENT_TASK = 5;
    private static final int MAX_NUM_OF_FREQUENTLY_USED = 8;

    private static final ComponentName[] PRESET_SYSTEM_APPS = {
        new ComponentName("com.android.settings", "com.android.settings.Settings"),                             // Settings
        new ComponentName("com.yunos.alicontacts", "com.yunos.alicontacts.activities.PeopleActivity2"),         // Contact
        new ComponentName("com.aliyun.wireless.vos.appstore", "com.aliyun.wireless.vos.appstore.LogoActivity"), // AppStore
        new ComponentName("com.yunos.theme.thememanager", "com.yunos.theme.thememanager.ThemeLogoActivity"),    // ThemeCenter
        new ComponentName("com.UCMobile", "com.UCMobile.main.UCMobile"),                                        // Browser
        new ComponentName("com.yunos.camera", "com.yunos.camera.CameraActivity"),                               // Camera
        new ComponentName("com.aliyun.SecurityCenter", "com.aliyun.SecurityCenter.ui.SecurityCenterActivity"),  // SecurityCenter
        new ComponentName("fm.xiami.yunos", "fm.xiami.bmamba.activity.StartActivity"),                          // Music
        new ComponentName("com.aliyun.video", "com.aliyun.video.VideoCenterActivity"),                          // Video
    };

    private enum Strategy {
        INITIAL,    // Suggest: preset system apps
        FIRST_DAY,  // Suggest: new installed > recent use > system apps
        NORMAL      // Suggest: new installed > frequently use > recent use > system apps
    }

    public static List<ShortcutInfo> getSuggestedApps(Context context, final int limit) {
        Log.d(TAG, "getSuggestedApps in: " + limit);
        context = (context == null) ?
                  LauncherApplication.getContext() : context.getApplicationContext();
        AppLaunchManager appLaunch = AppLaunchManager.getInstance();
        if (appLaunch == null || !appLaunch.isInitialized()) {
            // workspace is not completely loaded
            Log.e(TAG, "getSuggestedApps out: workspace is not completely loaded");
            return Collections.emptyList();
        }

        // determine strategy
        final Strategy strategy = determineStrategy(context);
        Log.d(TAG, "getSuggestedApps strategy: " + strategy);
        if (strategy == Strategy.INITIAL) {
            // return preset list
            List<ShortcutInfo> result = getPresetSystemApps(limit);
            Log.d(TAG, "getSuggestedApps out: return preset list: " + result.size());
            return result;
        }

        List<ShortcutInfo> result = new ArrayList<ShortcutInfo>(limit);
        List<ShortcutInfo> hotAndHideseatApps = getAllHotseatAndHideseatApps();

        // Dimension 1: new apps
        {
            List<ShortcutInfo> newApps = NewInstallAppHelper.getAllNewApps(context);
            appLaunch.geLastLaunchTimeHelper().sortItemsByLastLaunchTime(newApps);
            if (newApps.size() > MAX_NUM_OF_NEW_APPS) {
                newApps.subList(MAX_NUM_OF_NEW_APPS, newApps.size()).clear();
            }
            result.addAll(newApps);
            Log.d(TAG, "getSuggestedApps dim1: new apps: " + newApps.size());
        }

        // Dimension 2: frequently used apps
        if (strategy != Strategy.FIRST_DAY && result.size() < limit) {
            List<ShortcutInfo> frequently = appLaunch.getSuggestedApps(MAX_NUM_OF_FREQUENTLY_USED, hotAndHideseatApps);
            frequently.removeAll(result);
            result.addAll(frequently);
            Log.d(TAG, "getSuggestedApps dim2: frequently used: " + frequently.size());
        }

        // Dimension 3: recent tasks
        if (result.size() < limit) {
            List<ShortcutInfo> recentTasks = getRecentTaskApps(MAX_NUM_OF_RECENT_TASK);
            recentTasks.removeAll(hotAndHideseatApps);
            recentTasks.removeAll(result);
            result.addAll(recentTasks);
            Log.d(TAG, "getSuggestedApps dim3: recent tasks: " + recentTasks.size());
        }

        // Dimension 4: system apps
        if (result.size() < limit) {
            List<ShortcutInfo> systemApps = getPresetSystemApps(PRESET_SYSTEM_APPS.length);
            systemApps.removeAll(result);
            result.addAll(systemApps);
            Log.d(TAG, "getSuggestedApps dim4: sys apps: " + systemApps.size());
        }

        // sort and limit result
        appLaunch.geLastLaunchTimeHelper().sortItemsByLastLaunchTime(result);

        if (result.size() > limit) {
            result.subList(limit, result.size()).clear();
        }

        result = Collections.unmodifiableList(result);

        NewInstallAppHelper.notifyAppsShown(context, result);

        Log.d(TAG, "getSuggestedApps out: " + result.size());
        return result;
    }

    private static Strategy determineStrategy(Context context) {
        final long ONE_DAY_IN_MINSEC = 86400000;
        final String FIRST_USE_KEY = "SUGGESTED_APPS_FIRST_USE";

        String key = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(key, Context.MODE_PRIVATE);
        boolean firstUse = sp.getBoolean(FIRST_USE_KEY, true);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long dbCreationTime = sp.getLong(LauncherProvider.DATABASE_CREATION_TIME, 0);
        if (dbCreationTime != 0 &&
            currentTime - dbCreationTime < ONE_DAY_IN_MINSEC) {
            if (firstUse) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(FIRST_USE_KEY, false);
                editor.commit();
                return Strategy.INITIAL;
            } else {
                return Strategy.FIRST_DAY;
            }
        } else {
            return Strategy.NORMAL;
        }
    }

    /**
     * Returns preset system apps (does not include hotseat/hideseat apps).
     * The result is ordered.
     */
    private static List<ShortcutInfo> getPresetSystemApps(int limit) {
        // create a sparse array of component name
        final int size = Math.min(PRESET_SYSTEM_APPS.length, limit);
        final SparseArray<ComponentName> components = new SparseArray<ComponentName>(size);
        for (int i = 0; i < size; i++) {
            components.put(i, PRESET_SYSTEM_APPS[i]);
        }
        final SparseArray<ShortcutInfo> shortcuts = new SparseArray<ShortcutInfo>(size);

        // map component name to shortcut info
        LauncherModel.traverseAllAppItems(new ItemVisitor() {
            @Override
            public boolean visitItem(ItemInfo item) {
                if (item instanceof ShortcutInfo) {
                    Intent intent = ((ShortcutInfo) item).intent;
                    if (intent != null) {
                        for (int i = 0; i < components.size(); i++) {
                            if (components.valueAt(i).equals(intent.getComponent())) {
                                // shortcut found
                                if (item.container != Favorites.CONTAINER_HOTSEAT &&
                                    item.container != Favorites.CONTAINER_HIDESEAT) {
                                    // add to result
                                    shortcuts.put(components.keyAt(i), (ShortcutInfo) item);
                                }
                                components.removeAt(i);
                                if (components.size() == 0) {
                                    return false; // all apps are found, stop traverse
                                }
                                break;
                            }
                        }
                    }
                }
                return true; // continue traverse
            }
        });

        // convert sparse array to list
        List<ShortcutInfo> result = new ArrayList<ShortcutInfo>(size);
        for (int i = 0; i < shortcuts.size(); i++) {
            result.add(shortcuts.valueAt(i));
        }
        return result;
    }

    private static List<ShortcutInfo> getAllHotseatAndHideseatApps() {
        final List<ShortcutInfo> result = new ArrayList<ShortcutInfo>();
        LauncherModel.traverseAllAppItems(new ItemVisitor() {
            @Override
            public boolean visitItem(ItemInfo item) {
                if (item instanceof ShortcutInfo &&
                    item.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                    ShortcutInfo shortcut = (ShortcutInfo) item;
                    if (item.container == Favorites.CONTAINER_HOTSEAT ||
                        item.container == Favorites.CONTAINER_HIDESEAT) {
                        result.add(shortcut);
                    }
                }
                return true;
            }
        });
        return result;
    }

    private static List<ShortcutInfo> getRecentTaskApps(int limit){
        final List<ShortcutInfo> result = new ArrayList<ShortcutInfo>(limit);
        for (RecentTaskInfo recentTaskInfo : getRecentTasks(limit)) {
            final Intent baseIntent = recentTaskInfo.baseIntent;
            LauncherModel.traverseAllAppItems(new ItemVisitor() {
                @Override
                public boolean visitItem(ItemInfo item) {
                    if (item instanceof ShortcutInfo) {
                        Intent intent = ((ShortcutInfo) item).intent;
                        if (baseIntent != null && intent != null) {
                            if (baseIntent.getComponent().equals(intent.getComponent())) {
                                // found item
                                result.add((ShortcutInfo) item);
                                return false; // break traverse
                            }
                        }
                    }
                    return true; // continue traverse
                }
            });
        }
        return result;
    }

    private static List<RecentTaskInfo> getRecentTasks(int limit) {
        ActivityManager activityManager = (ActivityManager) LauncherApplication.
                                          getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return activityManager.getRecentTasks(limit, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
        } else {
            // FIXME: On Android L, this method is deprecated and only available for system app.
            // We will modify framework to enable this method specifically for homeshell.
            return activityManager.getRecentTasks(limit,
                    ActivityManager.RECENT_IGNORE_UNAVAILABLE | ACA.ActivityManager.RECENT_INCLUDE_PROFILES);
        }
    }

    private SuggestedApps() {}

}
