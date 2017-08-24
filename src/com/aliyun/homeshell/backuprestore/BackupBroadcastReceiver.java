package com.aliyun.homeshell.backuprestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

import com.aliyun.homeshell.backuprestore.BackupManager.OnRestoreFinishListener;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.AllAppsList;
import com.aliyun.homeshell.LauncherProvider;
import com.aliyun.homeshell.ApplicationInfo;

import java.util.Collections;

public class BackupBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION_APPSTORE =
            "com.aliyun.vos.wireless.appstore.restoreAppDone";

    private static final String ACTION_XIAOYUNMI =
            "com.aliyun.xiaoyunmi.systembackup.RestoreAllApp";

    private static final String ACTION_HOMESHELL_BACKUP =
            "com.aliyun.homeshell.systembackup.RestoreAllApp";

    private static final String ACTION_BACKUP_APP_LIST = "com.aliyun.homeshell.backupAppList";

    private static final String RESTORE_DB_FILE = "restore.db";

    private static final String TAG = "HOMESHELL/BackupBroadcastReceiver";

    public static final String ACTION_RESTART_HOMESHELL =
            "com.aliyun.homeshell.restore.restart";

    private Context mContext;

    /*YUNOS BEGIN*/
    //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
    //restore error
    final Handler restorehandler = new Handler();
    /*YUNOS END*/

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        String action = intent.getAction();
        Log.d(TAG, "action = " + action);
        if (ACTION_APPSTORE.equals(action)) {
            ArrayList<String> appListNotFound = intent.getStringArrayListExtra("packageNames");

            for (String packageName : appListNotFound) {
                Log.d(TAG, "appListNotFound packageName = " + packageName);
                /*YUNOS BEGIN*/
                //##date:2014/01/10 ##author:hao.liuhaolh ##BugID: 84736
                // SD app icon position change in restore
                BackupManager.getInstance().addRestoreDoneApp(context, packageName);
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##date:2014/04/26##author:hao.liuhaolh@alibaba-inc.com##BugID:114988
                //find and remove one item folder after all restore app handled by appstore
                BackupManager.getInstance().addRestoreDownloadHandledApp(packageName);
                /*YUNOS END*/
            }
        }else if (ACTION_XIAOYUNMI.equals(action)||ACTION_RESTART_HOMESHELL.equals(action)) {
            boolean downloadnow = intent.getBooleanExtra("downloadnow", false);
            Log.d(TAG, "downloadnow = " + downloadnow);
            BackupManager.setIsRestoreAppFlag(context, downloadnow);

            /*YUNOS BEGIN*/
            //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
            //restore error
            restorehandler.post(new Runnable() {
            /*YUNOS END*/
                @Override
                public void run() {
                    doRestore();
                }
            });
        }else if (ACTION_HOMESHELL_BACKUP.equals(action)) {
            final Handler handler = new Handler();
            handler.post(new Runnable() {

                @Override
                public void run() {
                    requestDownLoadAppFromAppStore();

                }
            });
        }
    }

    private void requestDownLoadAppFromAppStore() {
        String recordStr = BackupUitil.getBackupAppListString("");

        String filteredAppList = getfilteredAppList(recordStr);
        Log.d(TAG, "requestDownLoadAppFromAppStore  filteredAppList = " + filteredAppList);

        if (filteredAppList.equals("{}")) {
            Log.d(TAG, "No application, set inrestore false");
            /*YUNOS BEGIN*/
            //##date:2013/12/25 ##author:hao.liuhaolh ##BugID: 73014
            // client yun icon lost after restore
            BackupManager.setRestoreFlag(mContext, false);
            BackupManager.getInstance().setIsInRestore(false);
            /*YUNOS END*/
            return;
        }

        addNeedRestoreAppList(filteredAppList);
        BackupManager.getInstance().setOnRestoreFinishListener(new OnRestoreFinishListener() {

            @Override
            public void onRestoreFinish() {
                Log.d(TAG, "onRestoreFinish");
                BackupManager.setRestoreFlag(mContext, false);
                BackupManager.getInstance().setIsInRestore(false);
            }
        });

        String requestStr = convertToRequestStr(filteredAppList);

        String action = ACTION_BACKUP_APP_LIST;
        String appList = BackupUitil.ACTION_BACKUP_APP_LIST_INTENT_KEY;
        Intent intent = new Intent(action);
        intent.putExtra(appList, requestStr);
        Log.d(TAG, "requestDownLoadAppFromAppStore  appList = " + requestStr);
        mContext.sendBroadcast(intent);
    }

    private void addNeedRestoreAppList(String appList) {
        JSONObject json = null;
        try {
            json = new JSONObject(appList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json != null) {
            Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                BackupManager.getInstance().addNeedRestoreApp(key);
            }
        }
    }

    private String convertToRequestStr(String str) {
        String retStr = "";
        JSONObject json = null;
        try {
            json = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json != null) {
            Iterator<String> iter = json.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                try {
                    retStr = retStr + key + "#" + json.get(key) + ";";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return retStr;
    }

    private String getfilteredAppList(String recordStr) {
        String filteredStr = "";
        JSONObject json = null;
        try {
            json = new JSONObject(recordStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (json != null) {
            List<ResolveInfo> allAppInfo = AllAppsList.getAllActivity(mContext);
            for (ResolveInfo resolveInfo : allAppInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                json.remove(packageName);
            }
            /*YUNOS BEGIN*/
            //##date:2013/12/08 ##author:hongxing.whx ##bugid: 72245
            // we don't need to download the following applications either
            json.remove("com.android.browser");
            json.remove("com.aliyun.homeshell");

            //BugID:5597858:remove freezen apps from filterlist
            List<ApplicationInfo> allFrozenApps = null;
            if (Hideseat.isHideseatEnabled()) {
                allFrozenApps = AppFreezeUtil.getAllFrozenApps(mContext.getApplicationContext());
            } else {
                allFrozenApps = Collections.emptyList();
            }
            for (ApplicationInfo info : allFrozenApps) {
                if (info.componentName == null) {
                    continue;
                }
                String pkgName = info.componentName.getPackageName();
                json.remove(pkgName);
            }

            filteredStr = json.toString();
            /*YUNOS END*/
        }

        return filteredStr;
    }

    protected void doRestore() {
        Log.d(TAG, "start final doRestore");
        /*YUNOS BEGIN*/
        //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
        //restore error
        if ((BackupUitil.isRestoring == true) && (BackupUitil.postCount <= 50)) {
            Log.d(TAG, "in restoring status");
            BackupUitil.postCount++;
            restorehandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doRestore();
                }
            }, 100);
            return;
        }
        BackupUitil.postCount = 0;
        BackupUitil.isRestoring = false;
        if (prepareRestoreFile() == true) {
            BackupManager.setRestoreFlag(mContext, true);
            LauncherProvider.clearLoadDefaultWorkspaceFlag();
            restartHomeshell();
        } else {
            BackupManager.setRestoreFlag(mContext, false);
        }
        /*YUNOS END*/
    }

    private void restartHomeshell() {
        Log.d(TAG, "restartHomeshell");
        Process.killProcess(Process.myPid());
    }

    /*YUNOS BEGIN*/
    //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
    //restore error
    private boolean prepareRestoreFile() {
    /*YUNOS END*/
        Log.d(TAG, "prepareRestoreFile start");

        File file = mContext.getDatabasePath(LauncherProvider.DATABASE_NAME);
        File backupFileDir = new File(mContext.getFilesDir() + "/backup/");
        backupFileDir.mkdir();
        File backupFile = new File(backupFileDir, RESTORE_DB_FILE);

        Log.d(TAG, "backupFile: " + (backupFile == null ? "null" : backupFile.toString()));
        /*YUNOS BEGIN*/
        //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
        //restore error
        if ((backupFile == null) || (backupFile.exists() == false)) {
            return false;
        }
        try {
            BackupUitil.copyFile(backupFile, file);
        } catch (IOException e1) {
            e1.printStackTrace();
            Log.d(TAG, "prepareRestoreFile end with exception");
            return false;
        }
        Log.d(TAG, "prepareRestoreFile end");
        return true;
        /*YUNOS END*/
    }

}
