
package com.aliyun.homeshell;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class AppUtil {

    static public boolean isSystemApp(ResolveInfo resolveInfo) {
        return isSystemApp(resolveInfo.activityInfo.applicationInfo);
    }

    static public boolean isSystemApp(Context context, String packagename) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo appinfo = null;
        try {
            appinfo = pm.getApplicationInfo(packagename, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
        }
        return isSystemApp(appinfo);
    }

    static public boolean isSystemApp(ApplicationInfo appinfo) {
        if (appinfo == null) {
            return false;
        }
        int flags = appinfo.flags;
        if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return true;
        } else if ((flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true;
        }
        return false;
    }
}
