
package com.aliyun.homeshell.appclone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.aliyun.homeshell.ItemInfo;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.profilemanager.AliUserManager;
import com.aliyun.profilemanager.IAddAppCallback;
import com.aliyun.profilemanager.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AppCloneManager {
    private static final String TAG = "AppCloneManager";
    public static final boolean DEBUG = true;
    public static final int MAX_APP_CLONE_COUNT = 7;
    public static final int EXEC_SUCCESS = 1;// PackageManager.INSTALL_SUCCEEDED
    public static final int EXEC_FAILED = -1;

    private AliUserManager mAliUserManager;
    private HashMap<CloneAppKey, CloneApplicationInfo> mCloneAppMap;
    private int[] mUserIds;
    private boolean mUpdateMapLater = false;
    private volatile static AppCloneManager sAppCloneManager;
    private Handler mHandler;
    private Integer mMaxCloneCount;
    private static Boolean sSupportAppClone = null;
    private ShortcutInfo mCloningItem;
    private long mCloneStartTime;
    private static final int CLONE_TIME_MAX = 5 * 60 * 1000;

    public AppCloneManager() {
        if (isSupportAppClone()) {
            mAliUserManager = AliUserManager.get();
        }
        mCloneAppMap = new HashMap<CloneAppKey, CloneApplicationInfo>();

        updateCloneAppMap();
        mHandler = new Handler();
    }

    public static AppCloneManager getInstance() {

        if (sAppCloneManager == null) {
            synchronized (AppCloneManager.class) {
                if (sAppCloneManager == null) {
                    sAppCloneManager = new AppCloneManager();
                }
            }
        }
        return sAppCloneManager;
    }

    public boolean canClone(String packageName) {
        return isSupportAppClone() && Utils.isCloneApp(packageName)
                && mAliUserManager.canClone(packageName);
    }

    public boolean clone(final ShortcutInfo cloneInfo, final AppCloneCallback cb) {

        final String packageName = cloneInfo.getPackageName();
        Log.d(TAG, "clone : " + packageName);

        if (!isSupportAppClone()) {
            return false;
        }

        if (packageName == null || packageName.length() <= 0) {
            return false;
        }

        final IAddAppCallback packageCloneCb = new IAddAppCallback.Stub() {

            @Override
            public void onAddAppSuccessed(final int profileId, final String pkg)
                    throws RemoteException {
                Log.d(TAG, "onAddAppSuccessed() packageName:" + pkg + ", profileId:"
                        + profileId);
                mCloningItem = null;
                mCloneStartTime = 0;
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        if (!sAppCloneManager.mUpdateMapLater) {
                            sAppCloneManager.updateCloneAppMap();
                        }
                        if (packageName == null || !packageName.equals(pkg)) {
                            Log.e(TAG, "ERROR! packageDeleted, delete: " + packageName
                                    + ", but callback with: " + pkg);
                        } else if (cb != null) {
                            cb.onCloned(profileId, packageName, EXEC_SUCCESS);
                        }
                    }

                };
                mHandler.post(r);
            }

            @Override
            public void onAddAppFailed(final String pkg, String errormsg) throws RemoteException {
                Log.d(TAG, "onAddAppFailed() packageName:" + pkg + ", errormsg:"
                        + errormsg);
                mCloningItem = null;
                mCloneStartTime = 0;
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        if (packageName == null || !packageName.equals(pkg)) {
                            Log.e(TAG, "ERROR! packageDeleted, delete: " + packageName
                                    + ", but callback with: " + pkg);
                        } else if (cb != null) {
                            sAppCloneManager.updateCloneAppMap();
                            cb.onCloned(-1, pkg, EXEC_FAILED);
                        }
                    }

                };
                mHandler.post(r);
            }

        };

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "start clone : " + packageName + ", from userId:" + cloneInfo.userId);
                if (mAliUserManager.addProfileApp(packageName, packageCloneCb)) {
                    mCloningItem = cloneInfo;
                    Log.d(TAG, "cloning... " + packageName);
                }
            }
        };
        long currentTime = System.currentTimeMillis();
        if (mCloningItem != null && (currentTime - mCloneStartTime) > CLONE_TIME_MAX) {
            Log.w(TAG, "WARNNING, clone time out, startTime:" + mCloneStartTime + " item:"
                    + mCloningItem);
            mCloningItem = null;
            mCloneStartTime = 0;
        }
        if (mCloningItem != null) {
            Log.d(TAG, "still cloning : " + mCloningItem.getPackageName() + ", from userId:"
                    + mCloningItem.userId);
        } else {
            mCloningItem = cloneInfo;
            mCloneStartTime = System.currentTimeMillis();
            Thread t = new Thread(r, "AppCloneThread");
            t.start();
            return true;
        }

        Log.d(TAG, "can not clone: " + packageName + ", from userId:" + cloneInfo.userId);
        return false;
    }

    public boolean delete(final int userId, final String packageName, final AppCloneCallback cb) {

        Log.d(TAG, "delete : " + packageName + " userId:" + userId);
        if (!isSupportAppClone()) {
            return false;
        }

        IPackageDeleteObserver packageDelCb = new IPackageDeleteObserver.Stub() {

            @Override
            public void packageDeleted(final String pkg, final int returnCode)
                    throws RemoteException {

                Log.d(TAG, "packageDeleted() packageName:" + packageName + ", returnCode:"
                        + returnCode);
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        if (packageName == null || !packageName.equals(pkg)) {
                            Log.e(TAG, "ERROR! packageDeleted, delete: " + packageName
                                    + ", but callback with: " + pkg);
                        } else if (cb != null) {
                            sAppCloneManager.updateCloneAppMap();
                            cb.onDeleted(userId, pkg, returnCode);
                        }
                    }

                };
                mHandler.post(r);
            }

        };
        return mAliUserManager.deleteProfileApp(userId, packageName, packageDelCb);
    }

    public boolean deleteAll(String packageName, AppCloneCallback cb) {
        if (packageName == null) {
            return false;
        }
        Set<CloneAppKey> set = mCloneAppMap.keySet();
        int indicator = set.size() - 1;
        for (CloneAppKey key : set) {
            if (packageName.equals(key.packageName)) {
                delete(key.userId, packageName, indicator == 0 ? cb : null);
            }
            indicator--;
        }

        return true;
    }

    public boolean deleteAll(AppCloneCallback cb) {
        Set<CloneAppKey> set = mCloneAppMap.keySet();

        int indicator = set.size() - 1;
        for (CloneAppKey key : set) {
            delete(key.userId, key.packageName, indicator == 0 ? cb : null);
            indicator--;
        }

        return true;
    }

    public boolean launch(int userId, String packageName) {
        if (!isSupportAppClone()) {
            return false;
        }
        return mAliUserManager.launchProfileApp(userId, packageName);
    }

    @SuppressLint("UseSparseArrays")
    private void updateCloneAppMap() {
        Map<Integer, List<ApplicationInfo>> map = isSupportAppClone() ? mAliUserManager
                .getAllProfileApps() : null;
        if (map == null) {
            map = new HashMap<Integer, List<ApplicationInfo>>();
        }
        Set<Integer> idSet = map.keySet();

        mUserIds = new int[Math.max(MAX_APP_CLONE_COUNT, idSet.size())];

        int i = 0;
        for (int id : idSet) {
            mUserIds[i] = id;
            i++;
        }
        Arrays.sort(mUserIds);
        if (mUserIds.length > MAX_APP_CLONE_COUNT) {
            Log.e(TAG, "ERROR! mUserIds.length > MAX_CLONE_APP_COUNT :" + mUserIds.length);
            return;
        }

        List<ApplicationInfo> appList;
        mCloneAppMap.clear();
        for (Integer id : idSet) {
            appList = map.get(id);
            for (ApplicationInfo appInfo : appList) {
                CloneAppKey key = new CloneAppKey(id, appInfo.packageName);
                mCloneAppMap.put(key, new CloneApplicationInfo(appInfo, id));
            }
        }
    }

    public void onUpdate() {
        updateCloneAppMap();
    }

    public void onAppUninstalled(String packageName) {
        if (packageName == null || packageName.length() <= 0) {
            return;
        }
        Iterator<Entry<CloneAppKey, CloneApplicationInfo>> it = mCloneAppMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<CloneAppKey, CloneApplicationInfo> entry = it.next();
            if (packageName.equals(entry.getKey().packageName)) {
                it.remove();
                Log.d(TAG, "onAppUninstalled() remove:" + packageName + " id:"
                        + entry.getKey().userId);
            }
        }
    }

    public static int getMarkIconIndex(int userId, String packageName) {

        return userId % MAX_APP_CLONE_COUNT;
    }

    public int getCloneCount(String packageName) {
        int count = 0;
        if (packageName == null) {
            return count;
        }
        Set<CloneAppKey> keySet = mCloneAppMap.keySet();

        for (CloneAppKey key : keySet) {
            if (packageName.equals(key.packageName)) {
                count++;
            }
        }
        return count;
    }

    public Collection<CloneApplicationInfo> getAllCloneAppInfo() {
        return mCloneAppMap.values();
    }

    public List<CloneAppKey> getAllCloneApp() {
        List<CloneAppKey> keys = new ArrayList<CloneAppKey>();
        keys.addAll(mCloneAppMap.keySet());
        return keys;
    }

    public boolean addAllCloneResolveInfo(List<ResolveInfo> resolveInfoList) {
        if (resolveInfoList == null || resolveInfoList.size() <= 0) {
            return false;
        }
        Set<CloneAppKey> keySet = mCloneAppMap.keySet();
        List<CloneResolveInfo> cloneResolveInfoList = new ArrayList<CloneResolveInfo>();
        for (CloneAppKey key : keySet) {
            for (ResolveInfo rInfo : resolveInfoList) {
                if (key.packageName.equals(rInfo.activityInfo.packageName)) {
                    CloneResolveInfo info = new CloneResolveInfo(rInfo, key.userId);
                    cloneResolveInfoList.add(info);
                }
            }
        }
        resolveInfoList.addAll(cloneResolveInfoList);
        return true;
    }

    public static boolean isCloneShortcutInfo(Object o) {
        if (o instanceof ShortcutInfo && ((ShortcutInfo) o).userId > 0
                && ((ShortcutInfo) o).itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return true;
        }
        return false;
    }

    public boolean canClone(ShortcutInfo info) {
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                && canClone(info.getPackageName())) {
            return true;
        }
        return false;
    }

    public static boolean isClonable(String packageName) {
        return AppCloneManager.isSupportAppClone() && Utils.isCloneApp(packageName);
    }

    public boolean hasCloneBody(String packageName) {
        if (packageName == null) {
            return false;
        }
        Set<CloneAppKey> keySet = mCloneAppMap.keySet();
        for (CloneAppKey key : keySet) {
            if (packageName.equals(key.packageName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCloneApplicationInfo(ItemInfo itemInfo) {
        if (itemInfo instanceof com.aliyun.homeshell.ApplicationInfo
                && ((com.aliyun.homeshell.ApplicationInfo) itemInfo).userId > 0) {
            return true;
        }
        return false;
    }

    public boolean showClonableMark(ItemInfo itemInfo) {
        if (itemInfo instanceof ShortcutInfo) {
            ShortcutInfo shortcut = (ShortcutInfo) itemInfo;
            if (!shortcut.isSystemApp && !isCloneShortcutInfo(shortcut)
                    && (canClone(shortcut) || hasCloneBody(shortcut.getPackageName()))) {
                return true;
            }
        }
        return false;
    }

    public void updateMaxCloneCount(Context context) {
        int id = context.getResources().getIdentifier("config_multiuserMaximumUsers", "integer",
                "android");
        mMaxCloneCount = context.getResources().getInteger(id) - 1;
    }

    public boolean reachCountLimit(Context context, String packageName) {
        if (mMaxCloneCount == null) {
            updateMaxCloneCount(context);
        }
        if (getCloneCount(packageName) >= mMaxCloneCount) {
            return true;
        }
        return false;
    }

    public static boolean isSupportAppClone() {
        if (sSupportAppClone == null) {
            sSupportAppClone = "yes".equals(android.os.SystemProperties.get(
                    "ro.yunos.support.multiuserapps", "no"));
        }
        return sSupportAppClone;
    }

    public static class CloneAppKey {
        public int userId;
        public String packageName;

        public CloneAppKey(int userId, String packageName) {
            this.userId = userId;
            this.packageName = packageName;
        }

        public CloneAppKey(String packageName) {
            this.userId = 0;
            this.packageName = packageName;
        }

        public CloneAppKey(CloneApplicationInfo caInfo) {
            this.userId = caInfo.userId;
            this.packageName = caInfo.packageName;
        }

        @Override
        public int hashCode() {
            return (packageName == null ? 0 : packageName.hashCode()) + userId * 100;
        }

        @Override
        public String toString() {
            return super.toString() + " packageName:" + packageName + " userId:" + userId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CloneAppKey)) {
                return false;
            }
            CloneAppKey p = (CloneAppKey) o;
            return p.userId == userId && p.packageName.equals(packageName);
        }

    }

    public static class AppCloneCallback {

        public void onCloned(int userId, String packageName, int returnCode) {

        }

        public void onDeleted(int userId, String packageName, int returnCode) {

        }
    }

    /**
     * Called {@link com.aliyun.homeshell.Launcher#onDestroy()}.
     */
    public void destroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public ShortcutInfo getCloningItem() {
        return mCloningItem;
    }
}
