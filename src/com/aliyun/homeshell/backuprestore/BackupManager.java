package com.aliyun.homeshell.backuprestore;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;

import com.aliyun.homeshell.utils.HLog;

//BugID:114988:find and remove one item folder after all restore app handled by appstore
import com.aliyun.homeshell.model.LauncherModel;


public class BackupManager {

    public static final String RESTORE_STATUS = "restore_status";
    public static final String IN_RESTORE = "in_restore";
    public static final String RESTORE_APP = "restore_app";
    public static final String LAST_BACKUP_NUM = "last_backup_num";
    public static final String IS_FIRST_BACKUP = "is_first_backup";
    public static final String ACCOUNT_NOW = "account_now";
    private ArrayList<String> mAppList;
    private static BackupManager m_instance;

    //BugID:114988:find and remove one item folder after all restore app handled by appstore
    private ArrayList<String> mNeedDownloadAppList;

    private BackupManager(){
        mAppList = new ArrayList<String>();
        //BugID:114988:find and remove one item folder after all restore app handled by appstore
        mNeedDownloadAppList = new ArrayList<String>();
    }

    public static BackupManager getInstance(){
        if (m_instance == null) {
            m_instance = new BackupManager();
        }
        return m_instance;

    }

    public interface OnRestoreFinishListener{
        public void onRestoreFinish();
    }

    private OnRestoreFinishListener mOnRestoreFinishListener;

    public void setOnRestoreFinishListener(OnRestoreFinishListener l){
        mOnRestoreFinishListener = l;
    }

    private boolean m_isInRestore = false;

    public void addNeedRestoreApp(String packageName){
        HLog.d("JC", "add restore packageName = " + packageName);
        mAppList.add(packageName);
        //BugID:114988:find and remove one item folder after all restore app handled by appstore
        mNeedDownloadAppList.add(packageName);
    }

    /*YUNOS BEGIN*/
    //##date:2014/04/26##author:hao.liuhaolh@alibaba-inc.com##BugID:114988
    //find and remove one item folder after all restore app handled by appstore
    public void addRestoreDownloadHandledApp(String packageName){
        HLog.d("JC", "done restore download handled packageName = " + packageName);
        int leavecount = mNeedDownloadAppList.size();
        mNeedDownloadAppList.remove(packageName);

        if ((mNeedDownloadAppList.size() <= 0) && (leavecount > 0)) {
            LauncherModel.restoreDownloadHandled();
        }
    }
    /*YUNOS END*/

    public void addRestoreDoneApp(Context context, String packageName){
        if(TextUtils.isEmpty(packageName)){
            return;
        }
        HLog.d("JC", "done restore packageName = " + packageName);
        /* YUNOS BEGIN */
        // ## date: 2016/11/4 ## author: yongxing.lyx
        // ## BugID:9110196:if packageName isn't in restore list, just do nothing.
        // restoreDownloadHandled will check and delete folder has only one item.
        if (mAppList.remove(packageName)) {
            /*YUNOS BEGIN*/
            //##date:2014/01/10 ##author:hao.liuhaolh ##BugID: 84736
            // SD app icon position change in restore
            try {
                BackupUitil.removePackageFromAllAppListPreference(context, packageName);
            } catch(Exception e) {
                e.printStackTrace();
            }
            /*YUNOS END*/
            if (isRestoreDone()) {
                //when restore finish, do the clean op
                LauncherModel.restoreDownloadHandled();
                if (mOnRestoreFinishListener != null) {
                    mOnRestoreFinishListener.onRestoreFinish();
                }
            }
        } else {
            HLog.d("JC", "do nothing, it isn't in restore list : " + packageName);
        }
        /* YUNOS END */
    }

    public boolean isInRestorePendingList(String packageName){
        return mAppList.contains(packageName);
    }

    private boolean isRestoreDone() {
        if (mAppList.size() == 0) {
            return true;
        }
        return false;
    }

    public void setIsInRestore(boolean flag){
        m_isInRestore = flag;
    }

    public boolean isInRestore(){
        return m_isInRestore ;
    }

    public static void setRestoreFlag(Context ctx, boolean flag){
        ctx.getSharedPreferences(RESTORE_STATUS, 0).edit()
            .putBoolean(IN_RESTORE, flag).commit();
    }

    public static boolean getRestoreFlag(Context ctx){
        return ctx.getSharedPreferences(RESTORE_STATUS, 0).getBoolean(IN_RESTORE, false);
    }

    public static void setIsRestoreAppFlag(Context ctx, boolean flag){
        ctx.getSharedPreferences(RESTORE_STATUS, 0).edit()
            .putBoolean(RESTORE_APP, flag).commit();
    }

    public static boolean getIsRestoreAppFlag(Context ctx){
        return ctx.getSharedPreferences(RESTORE_STATUS, 0).getBoolean(RESTORE_APP, false);
    }

    public static void setLastBackupNum(Context ctx, int num){
        ctx.getSharedPreferences(RESTORE_STATUS, 0).edit()
            .putInt(LAST_BACKUP_NUM, num).commit();
    }

    public static int getLastBackupNum(Context ctx){
        return ctx.getSharedPreferences(RESTORE_STATUS, 0).getInt(LAST_BACKUP_NUM, 0);
    }

    public static void setIsFirstBackup(Context ctx, boolean flag){
        ctx.getSharedPreferences(RESTORE_STATUS, 0).edit()
            .putBoolean(IS_FIRST_BACKUP, flag).commit();
    }

    public static boolean getIsFirstBackup(Context ctx){
        return ctx.getSharedPreferences(RESTORE_STATUS, 0).getBoolean(IS_FIRST_BACKUP, true);
    }

    public static void setAccountNow(Context ctx, String account){
        ctx.getSharedPreferences(RESTORE_STATUS, 0).edit()
            .putString(ACCOUNT_NOW, account).commit();
    }

    public static String getAccountNow(Context ctx){
        return ctx.getSharedPreferences(RESTORE_STATUS, 0).getString(ACCOUNT_NOW, "");
    }

}

