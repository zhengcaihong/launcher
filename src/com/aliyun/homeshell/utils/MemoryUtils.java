package com.aliyun.homeshell.utils;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Debug;

public class MemoryUtils {
    private static final Uri CONTENT_URI_ACC_USER_WHITE_APP = Uri
            .parse("content://com.aliyun.provider.secure/pkg_acc_user_white_apps");
    private static final String PACKAGE_NAME = "packagename";

    public static long getMemoryPercent(final Context ctx) {
        MemoryInfo memoryInfo = new MemoryInfo();
        ActivityManager am = null;
        am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(memoryInfo);
        long avaialable = memoryInfo.availMem;
        long total = memoryInfo.totalMem;
        long used = total - avaialable;
        long percent = Math.round((double) (used * 100) / (double) total);
        return percent;
    }

    public static long getDstMemoryPercent(Context context) {
        // TODO Auto-generated method stub
        long memRelease = 0;
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
        for (int index = 0; index < apps.size(); index++) {
            String[] pkgList = apps.get(index).pkgList;
            for (int k = 0; k < pkgList.length; k++) {
                if (!isTaskLocked(context, pkgList[k])) {
                    memRelease += getPackageAvailabelMemory(context,
                            pkgList[k]);
                }
            }
        }
        long total = getTotalMemory(context);
        long willAvailable = getSystemAvaialbeMemorySize(context)
                + memRelease * 1024;
        long percent = 100 - (willAvailable * 100) / total;
        return percent;
    }

    public static long getPackageAvailabelMemory(Context ctx, String packageName) {
        ActivityManager am = (ActivityManager) ctx
                .getSystemService(Context.ACTIVITY_SERVICE);
        int[] pids = new int[1];
        List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
        for (int index = 0; index < apps.size(); index++) {
            String[] pkgList = apps.get(index).pkgList;
            for (int k = 0; k < pkgList.length; k++) {
                if (packageName.equals(pkgList[k])) {
                    pids[0] = apps.get(index).pid;
                    Debug.MemoryInfo[] memoryInfos = am
                            .getProcessMemoryInfo(pids);
                    int privateDirty = memoryInfos[0].getTotalPrivateDirty();
                    int sharedDirty = memoryInfos[0].getTotalSharedDirty();
                    return privateDirty + sharedDirty;
                }
            }
        }
        return 0;
    }

    public static long getSystemAvaialbeMemorySize(Context ctx) {
        MemoryInfo memoryInfo = new MemoryInfo();
        ActivityManager am = null;
        am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        return memSize;
    }

    public static long getTotalMemory(Context ctx) {
        MemoryInfo memoryInfo = new MemoryInfo();
        ActivityManager am = null;
        am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        am.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem;
    }

    public static boolean isTaskLocked(Context ctx, String packageName) {
        ContentResolver resolver = ctx.getContentResolver();
        Cursor cursor = resolver.query(CONTENT_URI_ACC_USER_WHITE_APP, null,
                PACKAGE_NAME + "=?", new String[] {
                    packageName
                }, null);
        boolean isTaskLocked = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return isTaskLocked;
    }
}
