package com.aliyun.homeshell.hideseat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;

import com.aliyun.homeshell.ApplicationInfo;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.utility.FeatureUtility;

/* YUNOS BEGIN */
// ##date:2014/7/21 ##author:zhanggong.zg ##BugID:5244146
/**
 * This is an utility class that designed for Application-Freeze feature.
 * The frozen (a.k.a. disabled) application cannot be queried from
 * <code>PackageManager</code> by <code>Intent</code> any more. Therefore,
 * frozen apps are completely hidden across the system.
 * <p>
 * However, due to system permission limitation, the actual code of enabling
 * and disabling an app is implemented in system Settings app. This class
 * only send specified broadcasts to Settings to trigger that code. (See <code>
 * com.android.settings.aliyun.appfreeze.FreezeReceiver</code> in Settings)
 * <p>
 * An potential issue of this class:<br>There is no callback for
 * {@link #asyncFreezePackage(Context, String)} and {@link #asyncUnfreezePackage(Context, String)}
 * to check whether the app is frozen or unfrozen successfully. Currently, we
 * assume all operations are successfully done after a short period of time (e.g.
 * 1 or 2 seconds).
 * <p>
 * @see #getAllFrozenApps(Context) Retrieves all frozen apps
 * @see #isPackageFrozen(Context, String) Determines a specified package is frozen or not
 * @see #asyncFreezePackage(Context, String) Freeze an app
 * @see #asyncUnfreezePackage(Context, String) Unfreeze an app
 * @author zhanggong.zg
 */
public final class AppFreezeUtil {

    private static final String TAG = "AppFreeze";

    private static final String ACTION_FREEZE_APP = "com.aliyun.freeze_application";
    private static final String ACTION_UNFREEZE_APP = "com.aliyun.unfreeze_application";

    private AppFreezeUtil() {
    }

    /**
     * Retrieves all frozen applications.
     * <p/>
     * Due to <code>PackageManager</code>'s implementation issue, the frozen app might not be
     * retrieved during installation process (i.e., the user is updating an app). To solve this,
     * caller can pass in a group of potential frozen <code>ShortcutInfo</code>s by parameter
     * <code>potentials</code>.
     * @param context
     * @param potentials
     * @return a list of <code>ApplicationInfo</code>
     */
    public static List<ApplicationInfo> getAllFrozenApps(Context context, ShortcutInfo... potentials) {
        Map<String, ApplicationInfo> rst = new HashMap<String, ApplicationInfo>();
        PackageManager packageManager = context.getPackageManager();
        for(android.content.pm.ApplicationInfo info : packageManager.getInstalledApplications(0)) {
            if (!info.enabled) {
                try {
                    // NOTE: the component name below is incomplete, because the class name
                    // of frozen app cannot be retrieved. This information will be filled
                    // in until the app is unfreezed. (See AllAppsList.updatePackage() method)
                    ComponentName componentName = new ComponentName(info.packageName, "");
                    String label = info.loadLabel(packageManager).toString();
                    rst.put(info.packageName, new ApplicationInfo(packageManager, componentName, label, null));
                } catch (Exception ex) {
                    Log.e(TAG, "getAllFrozenApps exception: ", ex);
                }
            }
        }
        // ##date:2014/12/18 ##author:zhanggong.zg ##BugID:5638756
        // If a frozen app is being installed, it might not be retrieved by the code above.
        // Caller has to pass in all potential frozen apps to solve this problem.
        if (potentials != null && potentials.length > 0) {
            for (ShortcutInfo item : potentials) {
                ComponentName cmpt = item.intent != null ? item.intent.getComponent() : null;
                String pkg = cmpt != null ? cmpt.getPackageName() : null;
                if (!TextUtils.isEmpty(pkg) && !rst.containsKey(pkg) && isPackageFrozen(context, pkg)) {
                    ApplicationInfo info = new ApplicationInfo(packageManager, cmpt.clone(), String.valueOf(item.title), null);
                    rst.put(pkg, info);
                    Log.d(TAG, "getAllFrozenApps: frozen app found: " + pkg);
                }
            }
        }
        return Arrays.asList(rst.values().toArray(new ApplicationInfo[rst.size()]));
    }

    /**
     * Determines that the specified <code>packageName</code> is frozen or not.
     * @param context
     * @param packageName
     * @return <code>true</code> if the package is frozen; <code>false</code> if
     * the package is not frozen or the specified <code>packageName</code> does
     * not exist.
     */
    public static boolean isPackageFrozen(Context context, String packageName) {
        if(!Hideseat.isHideseatEnabled()){
            return false;
        }
        if (context == null || TextUtils.isEmpty(packageName)) return false;
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(packageName, 0);
            if (info != null && info.applicationInfo != null) {
                return !info.applicationInfo.enabled;
            } else {
                return false;
            }
        } catch (NameNotFoundException ex) {
            // ##date:2014/12/18 ##author:zhanggong.zg ##BugID:5638756
            // getPackageInfo() method might throw NameNotFoundException during application
            // installation process. Use getApplicationEnabledSetting() method as a backup.
            try {
                int state = packageManager.getApplicationEnabledSetting(packageName);
                return (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            } catch (IllegalArgumentException ex2) {
                // The app is already deleted.
                Log.d(TAG, "isPackageFrozen exception on packageName: " + packageName
                           + " message: " + ex2.getMessage());
                return false;
            }
        }
    }

    /**
     * @param context
     * @param info
     * @see #isPackageFrozen(Context, String)
     */
    public static boolean isPackageFrozen(Context context, ShortcutInfo info) {
        if (info == null || info.intent == null) return false;
        ComponentName cmpt = info.intent.getComponent();
        if (cmpt == null) return false;
        return isPackageFrozen(context, cmpt.getPackageName());
    }

    /**
     * Sends a broadcast to Settings app to freeze an application package.
     * @param context
     * @param packageName
     */
    public static void asyncFreezePackage(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) return;
        Intent intent = new Intent(ACTION_FREEZE_APP);
        intent.putExtra("packageName", packageName);
        context.sendBroadcast(intent);
        Log.v(TAG, "asyncFreezePackage: " + packageName);
    }

    /**
     * Sends a broadcast to Settings app to unfreeze an application package.
     * @param context
     * @param packageName
     */
    public static void asyncUnfreezePackage(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) return;
        Intent intent = new Intent(ACTION_UNFREEZE_APP);
        intent.putExtra("packageName", packageName);
        context.sendBroadcast(intent);
        Log.v(TAG, "asyncUnfreezePackage: " + packageName);
    }

    public static void asyncUnfreezeAll(Context context) {
        for (ApplicationInfo info : getAllFrozenApps(context)) {
            asyncUnfreezePackage(context, info.componentName.getPackageName());
        }
    }

}
/* YUNOS END */
