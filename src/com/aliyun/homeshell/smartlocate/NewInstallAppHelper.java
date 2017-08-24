package com.aliyun.homeshell.smartlocate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.model.LauncherModel;
import com.aliyun.homeshell.model.LauncherModel.ItemVisitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * This class is used to determine new-installed apps in smart suggestion.<p>
 * A new-installed app is:
 * <ol>
 * <li>ShortcutInfo.isNew == true</li>
 * <li>Installation time is in 24 hours (see {@link #INSTALLATION_TIME_THRESHOLD})</li>
 * <li>Not being suggested over 2 times (see {@link #MAX_SHOWN_COUNT} and {@link #sShownCount})</li>
 * </ol>
 * @author zhanggong.zg
 */
public final class NewInstallAppHelper {

    private static final String TAG = "NewInstallAppHelper";

    private static final long INSTALLATION_TIME_THRESHOLD = 86400000; // 1 day (24 * 3600 * 1000)

    /**
     * This map stores the suggestion times for each apps. If an app is suggested
     * over 2 times (default value of {@link #MAX_SHOWN_COUNT}), the app won't be
     * suggested as new app again.<p>
     * This map is initialized in {@link #initShownCount(Context)}. The content of
     * this map is also stored in shared preference, see {@link #loadSuppressedApps(Context)}
     * and {@link #saveSuppressedApps(Context)}.
     */
    private static Map<Long, Integer> sShownCount = null; // id -> count

    /** @see #INSTALLATION_TIME_THRESHOLD */
    private static final int MAX_SHOWN_COUNT = 2;
    private static final String SUPPRESSED_ID_KEY = "SUGGESTED_APPS_SUPPRESSED_NEW_APP_ID";

    /**
     * Returns all new-installed apps (does not include hotseat/hideseat apps).
     * The result is not ordered.
     */
    public synchronized static List<ShortcutInfo> getAllNewApps(Context context) {
        initShownCount(context);
        final List<Long> suppressed = new ArrayList<Long>(sShownCount.size());
        final List<ShortcutInfo> result = new ArrayList<ShortcutInfo>();
        final PackageManager packageManager = context.getPackageManager();
        LauncherModel.traverseAllAppItems(new ItemVisitor() {
            @Override
            public boolean visitItem(ItemInfo item) {
                if (item instanceof ShortcutInfo &&
                    item.itemType == Favorites.ITEM_TYPE_APPLICATION) {
                    ShortcutInfo shortcut = (ShortcutInfo) item;
                    if (item.container == Favorites.CONTAINER_HOTSEAT ||
                        item.container == Favorites.CONTAINER_HIDESEAT) {
                        // ignore hotseat and hideseat
                        return true;
                    } else if (isNewApp(shortcut, packageManager)) {
                        if (!isSuppressed(shortcut)) {
                            result.add(shortcut);
                        } else {
                            suppressed.add(shortcut.id);
                        }
                    }
                }
                return true; // traverse all items
            }
        });
        sShownCount.keySet().retainAll(suppressed);
        return result;
    }

    /**
     * This method should be called when UI shows final suggested apps.
     * If an app is suggested over 2 times, it won't be suggested as new
     * app again (suppressed).
     * @see #isSuppressed(ShortcutInfo)
     */
    public synchronized static void notifyAppsShown(Context context, Collection<ShortcutInfo> suggestedApps) {
        if (sShownCount == null) {
            return;
        }
        for (ShortcutInfo item : suggestedApps) {
            if (item.isNewItem()) {
                Integer count = sShownCount.get(item.id);
                if (count == null) {
                    sShownCount.put(item.id, 1);
                } else {
                    sShownCount.put(item.id, count + 1);
                }
            }
        }
        saveSuppressedApps(context);
    }

    /**
     * This method is called when an app is uninstalled.
     */
    public synchronized static void notifyAppUninstalled(Context context, ItemInfo item) {
        if (item != null) {
            if (sShownCount != null && sShownCount.remove(item.id) != null) {
                saveSuppressedApps(context);
            }
        }
    }

    /**
     * Determines whether an item is newly installed in one day or not.
     */
    private static boolean isNewApp(ShortcutInfo item, PackageManager packageManager) {
        if (!item.isNewItem()) {
            return false;
        } else {
            // get installation time
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(item.getPackageName(), 0);
                long dt = Calendar.getInstance().getTimeInMillis() - packageInfo.firstInstallTime;
                return dt < INSTALLATION_TIME_THRESHOLD;
            } catch (NameNotFoundException e) {
                return false;
            }
        }
    }

    /**
     * @see #sShownCount
     */
    private static boolean isSuppressed(ShortcutInfo item) {
        Integer count = sShownCount.get(item.id);
        return count != null && count >= MAX_SHOWN_COUNT;
    }

    @SuppressLint("UseSparseArrays")
    private static void initShownCount(Context context) {
        if (sShownCount == null) {
            List<Long> suppressed = loadSuppressedApps(context);
            sShownCount = new HashMap<Long, Integer>(suppressed.size());
            for (Long id : suppressed) {
                sShownCount.put(id, MAX_SHOWN_COUNT);
            }
        }
    }

    private static List<Long> loadSuppressedApps(Context context) {
        String key = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(key, Context.MODE_PRIVATE);
        if (!sp.contains(SUPPRESSED_ID_KEY)) {
            return Collections.emptyList();
        }

        Set<String> set = sp.getStringSet(SUPPRESSED_ID_KEY, Collections.<String>emptySet());
        if (set.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<Long>(set.size());
        try {
            for (String idStr : set) {
                result.add(Long.valueOf(idStr));
            }
        } catch (NumberFormatException ex) {
            Log.e(TAG, "getSuppressedNewApps error", ex);
            Editor editor = sp.edit();
            editor.remove(SUPPRESSED_ID_KEY);
            editor.commit();
        }

        Log.d(TAG, "loadSuppressedApps loaded: " + result.size());
        return result;
    }

    private static void saveSuppressedApps(Context context) {
        Set<String> idSet = new HashSet<String>();
        for (Entry<Long, Integer> entry : sShownCount.entrySet()) {
            if (entry.getValue() >= MAX_SHOWN_COUNT) {
                idSet.add(entry.getKey().toString());
            }
        }

        String key = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(key, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putStringSet(SUPPRESSED_ID_KEY, idSet);
        editor.commit();
        Log.d(TAG, "saveSuppressedApps saved: " + idSet.size());
    }

    private NewInstallAppHelper() {}

}
