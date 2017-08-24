package com.aliyun.homeshell.backuprestore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.aliyun.ams.tyid.TYIDException;
import com.aliyun.ams.tyid.TYIDManager;
import com.aliyun.homeshell.AllAppsList;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherProvider;
import com.aliyun.homeshell.LauncherSettings.Favorites;
//import com.yunos.theme.thememanager.backup.ThemeBackupHelper;
import com.aliyun.homeshell.hideseat.AppFreezeUtil;
import com.aliyun.homeshell.hideseat.Hideseat;
import com.aliyun.homeshell.ApplicationInfo;

import java.util.Collections;

public class BackupAgent extends BackupAgentHelper{
    static final Object[] sDataLock = new Object[0];

    private static final String TAG = "HomeShellBackupAgent";

    private static final String ALL_APP_LIST = "all_app_list";

    private static final String BACKUP_DB_FILE1 = "backup1.db";
    private static final String BACKUP_DB_FILE2 = "backup2.db";
    private static final String RESTORE_DB_FILE = "restore.db";

    private int m_lastNum = 0;
    private String m_appList = "";

    private Context m_context = null;


    @Override
    public void onCreate() {
        Log.d(TAG, "HomeShellBackupAgent onCreate");
        m_context  = getApplicationContext();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
             ParcelFileDescriptor newState) throws IOException {
        Log.d(TAG, "HomeShellBackupAgent onBackup");

     // If this is the first backup ever, we have to back up everything
        boolean forceBackup = false;
        if (BackupManager.getIsFirstBackup(m_context)
                || oldState == null || isAccountChanged()) {
            forceBackup = true;
            BackupManager.setIsFirstBackup(m_context, false);
        }

        Log.d(TAG, "forceBackup = " + forceBackup+",oldState=" + oldState);

        // Now read the state as of the previous backup pass, if any
        int lastBackupNum = 0;
        String lastAllAppList = "";

        if (!forceBackup) {

            FileInputStream instream = new FileInputStream(oldState.getFileDescriptor());
            DataInputStream in = new DataInputStream(instream);

            try {
                // Read the state as of the last backup
                lastBackupNum = in.readInt();
                lastAllAppList = in.readLine();
                in.close();
                Log.d(TAG, "lastBackupNum" + lastBackupNum);
                Log.d(TAG, "lastAllAppList" + lastAllAppList);
//                lastAllAppList = new String(in.readLine().getBytes("UTF-8"), "UTF-8");
            } catch (IOException e) {
                // If something went wrong reading the state file, be safe and
                // force a backup of all the data again.
                Log.e(TAG, "met IOException");
                forceBackup = true;
            }
        }

        if (forceBackup) {
            // reset the backup data set
            wipeOldData(data);
        }

        doAppListBackup(lastAllAppList, data, forceBackup);

        doHomeShellMainDBBackup(lastBackupNum, data, forceBackup);

        copyOrigDBTOBackupDB();
        // Finally, write the state file that describes our data as of this backup pass
        writeStateFile(newState);
        Log.d(TAG, "HomeShellBackupAgent onBackup end: m_lastNum" + m_lastNum);
    }

    private boolean isAccountChanged() {
        Log.d(TAG, "isAccountChanged start");
        String account = "";
        try {
            account = TYIDManager.get(m_context).yunosGetLoginId();

        } catch (TYIDException e) {
            e.printStackTrace();
        }
        String accountNow = BackupManager.getAccountNow(m_context);
        if (account.equalsIgnoreCase(accountNow)) {
            Log.d(TAG, "account NOT change");
            return false;
        }else {
            BackupManager.setAccountNow(m_context, account);
            Log.d(TAG, "account change");
            return true;
        }
    }

    private void doHomeShellMainDBBackup(int lastBackupNum,
            BackupDataOutput data, boolean forceBackup)
            throws IOException {
        // Now read the state as of the previous backup pass, if any
        int newLastBackupNum = 0;

        boolean backupSuccess = false;
        if (forceBackup) {
            newLastBackupNum = 0;
        }else {
            int backupNum = BackupManager.getLastBackupNum(getApplicationContext());
            Log.d(TAG, "backupNum=" +backupNum +", lastBackupNum="+ lastBackupNum);
            if (backupNum == lastBackupNum) {
                backupSuccess = true;
                newLastBackupNum = lastBackupNum + 1;
            }else {
                backupSuccess = false;
                newLastBackupNum = lastBackupNum;
            }
        }

        HashMap<String, BackupRecord> addedList = new HashMap<String, BackupRecord>();
        HashMap<String, BackupRecord> modifiedList = new HashMap<String, BackupRecord>();
        HashMap<String, BackupRecord> deletedList = new HashMap<String, BackupRecord>();

        if (forceBackup) {
            getFullBackupSet(addedList, modifiedList, deletedList);
            copyOrigDBTOBackupDB();
            updateBackupDBFileOld();
        }else {
            String filename;
            if (backupSuccess) {
                filename = BACKUP_DB_FILE2;
            }else {
                filename = BACKUP_DB_FILE1;
            }

            getIncrementBackupSet(addedList, modifiedList, deletedList, filename);

            if (backupSuccess) {
                updateBackupDBFileOld();
                copyOrigDBTOBackupDB();
            }else {
                copyOrigDBTOBackupDB();
            }
        }

        backupData(data, addedList);
        Log.d(TAG, "addedList.size"+addedList.size());
        backupData(data, modifiedList);
        Log.d(TAG, "modifiedList.size"+modifiedList.size());
        deleteBackupedData(data, deletedList);
        Log.d(TAG, "deletedList.size"+deletedList.size());

        m_lastNum = newLastBackupNum;
        BackupManager.setLastBackupNum(getApplicationContext(), newLastBackupNum);
        Log.d(TAG, "backupSuccess = " + backupSuccess +
                " m_lastNum = " + m_lastNum);
    }

    private void doAppListBackup(String lastAllApplist,
            BackupDataOutput data, boolean forceBackup) {
        boolean appListNeedBackup = true;

        String allAppList = "";
        allAppList = getAllAppList();
        Log.d(TAG, "doAppListBackup: allAppList="+allAppList);

        if (lastAllApplist != null) {
            appListNeedBackup = (false == allAppList.contentEquals(lastAllApplist));
        }

        Log.d(TAG, "doAppListBackup: appListNeedBackup="+appListNeedBackup);

        if (forceBackup || appListNeedBackup) {
            Log.d(TAG, "doAppListBackup: backup allAppList = " + allAppList);
            backupData(data, allAppList, ALL_APP_LIST);
        }

        m_appList = allAppList;
        Log.d(TAG, "doAppListBackup: backup m_appList = " + m_appList);
    }

    /**
     * Write out the new state file:  the version number, followed by the
     * three bits of data as we sent them off to the backup transport.
     */
    void writeStateFile(ParcelFileDescriptor stateFile, String state) throws IOException {
        FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outstream);
        out.writeBytes(state);
        out.flush();
        out.close();
    }

    /**
     * Write out the new state file:  the version number, followed by the
     * three bits of data as we sent them off to the backup transport.
     */
    void writeStateFile(ParcelFileDescriptor stateFile) throws IOException {
        FileOutputStream outstream = new FileOutputStream(stateFile.getFileDescriptor());
        DataOutputStream out = new DataOutputStream(outstream);
        out.writeInt(m_lastNum);
        out.writeBytes(m_appList);
        out.flush();
        out.close();
    }

      private String getAllAppList(){
      JSONObject allAppList = new JSONObject();
      List<ResolveInfo> allAppInfo = getAllApps(getApplicationContext());
      for (ResolveInfo resolveInfo : allAppInfo) {
          String packageName = resolveInfo.activityInfo.packageName;
          String applicationName = (String) resolveInfo.activityInfo.applicationInfo.loadLabel(getPackageManager());
          try {
            allAppList.put(packageName, applicationName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
      }

      //BugID:5597858:add freezen apps in all app list
      List<ApplicationInfo> allFrozenApps = null;
      if (Hideseat.isHideseatEnabled()) {
          allFrozenApps = AppFreezeUtil.getAllFrozenApps(getApplicationContext());
      } else {
          allFrozenApps = Collections.emptyList();
      }
      for (ApplicationInfo info : allFrozenApps) {
          if (info.componentName == null) {
              continue;
          }
          String pkgName = info.componentName.getPackageName();
          String applicationName = info.title.toString();
          try {
              allAppList.put(pkgName, applicationName);
          } catch (JSONException e) {
              e.printStackTrace();
          }
      }

      return allAppList.toString();
    }

    private List<ResolveInfo> getAllApps(Context context){
        List<ResolveInfo> allApps = AllAppsList.getAllActivity(context);
        return allApps;
    }

    private void wipeOldData(BackupDataOutput data) {
        for (int i = 0; i < 500; i++) {
            try {
                deleteBackupEntity(data, Integer.valueOf(i).toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void backupData(BackupDataOutput data,
            HashMap<String, BackupRecord> datalist) throws IOException {
        Iterator iter = datalist.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            BackupRecord record = (BackupRecord) entry.getValue();
            writeBackupEntity(data, record, key);
            Log.d(TAG, "backupData key = " + key);

        }
    }

    private void backupData(BackupDataOutput data, String value, String key){
        byte[] valueData = value.getBytes();
        int recordSize = valueData.length;
        Log.d(TAG, "backupData key="+ key +",length = " + recordSize);
        try {
            data.writeEntityHeader(key, recordSize);
            data.writeEntityData(valueData, recordSize);
        } catch (IOException e) {
            Log.d(TAG, "backupData meet exception:"+e);
            e.printStackTrace();
        }
    }

    private void deleteBackupedData(BackupDataOutput data,
            HashMap<String, BackupRecord> datalist) throws IOException {
        Iterator iter = datalist.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            deleteBackupEntity(data, key);
            Log.d(TAG, "deleteBackupedData key = " +
                    key);
            }
    }

    void writeBackupEntity(BackupDataOutput data, BackupRecord record, String key)
            throws IOException {
        byte[] recordData = record.getValue().toString().getBytes();
        int recordSize = recordData.length;
        data.writeEntityHeader(key, recordSize);
        data.writeEntityData(recordData, recordSize);
    }

    void deleteBackupEntity(BackupDataOutput data, String key)
            throws IOException {
        data.writeEntityHeader(key, -1);
    }

    private void getFullBackupSet(HashMap<String, BackupRecord> addedList,
                                  HashMap<String, BackupRecord> modifiedList,
                                  HashMap<String, BackupRecord> deletedList) {
        HashMap<String, BackupRecord> origRecordList = new HashMap<String, BackupRecord>();
        HashMap<String, BackupRecord> newRecordList = new HashMap<String, BackupRecord>();

        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        //BugID:5595776:backup normal mode favorites table
        Cursor newc = contentResolver.query(Favorites.CONTENT_URI_NORMAL_MODE, null, null, null, null);
        newRecordList = BackupUitil.convertDBToBackupSet(newc);

        BackupUitil.collectDiffInfo(origRecordList, newRecordList, addedList, modifiedList, deletedList);
    }

    private void getIncrementBackupSet(HashMap<String, BackupRecord> addedList,
            HashMap<String, BackupRecord> modifiedList,
            HashMap<String, BackupRecord> deletedList,
            String filename) {
        HashMap<String, BackupRecord> origRecordList = new HashMap<String, BackupRecord>();
        HashMap<String, BackupRecord> newRecordList = new HashMap<String, BackupRecord>();

        File backupDBfile = new File(getApplicationContext().getFilesDir() + "/backup/" + filename);
        if (false == backupDBfile.exists()) {
        Log.d(TAG, "backupDBfile not exists, it must be the first time backup");
        }else {
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(backupDBfile, null);
            Cursor c = db.query("favorites", null, null, null, null, null, null);
            origRecordList = BackupUitil.convertDBToBackupSet(c);
            db.close();
        }

        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        //BugID:5595776:backup normal mode favorites table
        Cursor newc = contentResolver.query(Favorites.CONTENT_URI_NORMAL_MODE, null, null, null, null);
        newRecordList = BackupUitil.convertDBToBackupSet(newc);

        BackupUitil.collectDiffInfo(origRecordList, newRecordList, addedList, modifiedList, deletedList);
        Log.d(TAG, "end of compareDB addedList = " +
                addedList.size() +
                " modifiedList =" + modifiedList.size() +
                " deletedList =" + deletedList.size());
        }

    private void copyOrigDBTOBackupDB() {
        copyOrigDBTOBackupDB(BACKUP_DB_FILE2);
    }

    private void copyOrigDBTORestoreDB() {
        copyOrigDBTOBackupDB(RESTORE_DB_FILE);
    }

    private void copyOrigDBTOBackupDB(String filename) {
        Log.d(TAG, "copyOrigDBTOBackupDB");
        File file = LauncherApplication.getContext().getDatabasePath(LauncherProvider.DATABASE_NAME);
        File backupFileDir = new File(LauncherApplication.getContext().getFilesDir() + "/backup/");
        backupFileDir.mkdir();
        File backupFile = new File(backupFileDir, filename);
        try {
            boolean createfile = backupFile.createNewFile();
            Log.d(TAG, "createfile = " + createfile);
            BackupUitil.copyFile(file, backupFile);
        } catch (IOException e1) {
            Log.d(TAG, "copyFile throw exception");
            e1.printStackTrace();
        }
    }

    private void updateBackupDBFileOld(){
        Log.d(TAG, "updateBackupDBFile1");
        File backupFileDir = new File(getApplicationContext().getFilesDir() + "/backup/");
        backupFileDir.mkdir();
        File backupFile1 = new File(backupFileDir, BACKUP_DB_FILE1);
        File backupFile2 = new File(backupFileDir, BACKUP_DB_FILE2);
        try {
            backupFile1.createNewFile();
            backupFile2.createNewFile();
            BackupUitil.copyFile(backupFile2, backupFile1);
        } catch (IOException e1) {
            Log.d(TAG, "copyFile throw exception");
            e1.printStackTrace();
        }
    }

    /**
     * Adding locking around the file rewrite that happens during restore is
     * similarly straightforward.
     */
    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
            ParcelFileDescriptor newState) throws IOException {
        Log.d(TAG, "HomeShellBackupAgent onRestore, appVersionCode is " + appVersionCode);

        /*YUNOS BEGIN*/
        //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
        //restore error
        BackupUitil.isRestoring = true;
        /*YUNOS END*/
        BackupManager.setRestoreFlag(getApplicationContext(), true);

        HashMap<String,BackupRecord> backupRecordList = new HashMap<String, BackupRecord>();
        // Consume the restore data set, remembering each bit of application state
        // that we see along the way
        while (data.readNextHeader()) {
            String key = data.getKey();
            int dataSize = data.getDataSize();
            Log.d(TAG, "key:"+key+",dataSize:"+dataSize);

            /*YUNOS BEGIN*/
            //##module(HomeShell)
            //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
            //check restore data to avoid no item in dock
            if (dataSize <= 0) {
                continue;
            }
            /*YUNOS END*/

            byte[] dataBuf = new byte[dataSize];
            data.readEntityData(dataBuf, 0, dataSize);
            String recordStr = new String(dataBuf, "UTF-8");
            Log.d(TAG, "recordStr:"+recordStr);

            if (ALL_APP_LIST.equals(key)) {
                  Log.d(TAG, "Get all applications list");
                //save appList to TempSharePref
                BackupUitil.setBackupAppListString(recordStr);
                Log.d(TAG, "onRestore  appList recordStr = " + recordStr);
            }else {
                try {
                    BackupRecord record = new BackupRecord(new JSONObject(recordStr));
                    backupRecordList.put(key, record);
                    Log.d(TAG, "key = " + key + " record = " + recordStr);
                } catch (JSONException e) {
                    Log.d(TAG, "key = " + key + " recordStr = " + recordStr + "   is not a JSONString");
                    //e.printStackTrace();
                }
            }
        }

        /*YUNOS BEGIN*/
        //##date:2014/01/03 ##author:hao.liuhaolh ##BugID: 78954
        //icon lost after restore
        final int versioncode  = appVersionCode;
        restoreDB(backupRecordList, versioncode);
        /*YUNOS END*/

        // Hold the lock while the FileBackupHelper restores the file from
        // the data provided here.
        synchronized (sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }

    private void restoreDB(final HashMap<String,BackupRecord> list, final int appVersionCode) {
        Log.d(TAG, "restoreDB Start");

        new Thread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                Log.d(TAG, "restoreDB Start in another thread");
                // delete old db table
                copyOrigDBTORestoreDB();
                File restoreDBfile = new File(getApplicationContext().getFilesDir()
                        + "/backup/" + RESTORE_DB_FILE);
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(restoreDBfile, null);
                Log.d(TAG, "db.delete favorites begin");
                db.delete("favorites", null, null);
                Log.d(TAG, "db.delete favorites end");

                // convert restore file to db table

                /*YUNOS BEGIN*/
                //##module(HomeShell)
                //##date:2014/03/27 ##author:hao.liuhaolh@alibaba-inc.com##BugID:105009
                // icon arrange error if restore db failed
                boolean restorefailed = false;
                try {
                    if ((list != null) && (list.size() > 0)) {
                        /*YUNOS BEGIN*/
                        //##date:2014/01/03 ##author:hao.liuhaolh ##BugID: 78954
                        //icon lost after restore

                        /*YUNOS BEGIN*/
                        //##date:2014/03/18 ##author:hao.liuhaolh ##BugID:101711
                        //vp install item icon is default icon after restore
                        BackupUitil.convertBackupSetToDB(db, list, appVersionCode, m_context);
                        /*YUNOS END*/
                        /*YUNOS END*/
                    }else {
                        Log.d(TAG, "list = null");
                        restorefailed = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    restorefailed = true;
                }

                /*YUNOS BEGIN*/
                //##module(HomeShell)
                //##date:2014/03/28 ##author:nater.wg ##BugID:105796
                //update restore database for system applications
                if (restorefailed == false) {
                    try {
                        updateRestoreDb(db);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        restorefailed = true;
                    }
                }
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##module(HomeShell)
                //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
                //check restore data to avoid no item in dock
                if (restorefailed == false) {
                    restorefailed = isRestoreDataError(db);
                }
                /*YUNOS END*/
                db.close();

                // icon arrange error if restore db failed
                // if restore failed, delete the restore.db, so homeshell.db will not be
                // replace by a empty db in doRestore() in BackupBroadcastReceiver.java
                if ((restorefailed == true) && (restoreDBfile != null)) {
                    restoreDBfile.delete();
                }
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##date:2014/02/26 ##author:hao.liuhaolh ##BugID:94434
                //restore error
                BackupUitil.isRestoring = false;
                /*YUNOS END*/
                /*YUNOS BEGIN*/
                //##BugID:7876269 ##author:xiangnan.xn
                //##date:2016/03/03
                // send ACTION_RESTART_HOMESHELL broadcast to do final restore procedure
                Log.d(TAG,"send restart homeshell command");
                Intent intent = new Intent();
                intent.setAction(BackupBroadcastReceiver.ACTION_RESTART_HOMESHELL);
                LauncherApplication.getContext().sendBroadcast(intent);
                /*YUNOS END*/
            }
        }).start();
    }

    public void restoreDB(final ContentValues[] contentvalues, final int appVersionCode) {
        Log.d(TAG, "restoreDB Start");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "restoreDB Start in another thread");
                // delete old db table
                copyOrigDBTORestoreDB();
                File restoreDBfile = new File(LauncherApplication.getContext().getFilesDir()
                        + "/backup/" + RESTORE_DB_FILE);
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(restoreDBfile, null);
                db.delete("favorites", null, null);

                boolean restorefailed = false;
                if ((contentvalues != null) && (contentvalues.length > 0)) {
                    BackupUitil.convertBackupSetToDB(db, contentvalues, appVersionCode, m_context);
                }else {
                    Log.d(TAG, "contentvalues invaild");
                    restorefailed = true;
                }

                //update restore database for system applications
                if (restorefailed == false) {
                    try {
                        updateRestoreDb(db);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        restorefailed = true;
                    }
                }

                if (restorefailed == false) {
                    restorefailed = isRestoreDataError(db);
                }
                db.close();

                if ((restorefailed == true) && (restoreDBfile != null)) {
                    restoreDBfile.delete();
                }

                BackupUitil.isRestoring = false;

                Log.d(TAG,"send restart command");
                Intent intent = new Intent();
                intent.setAction(BackupBroadcastReceiver.ACTION_RESTART_HOMESHELL);
                LauncherApplication.getContext().sendBroadcast(intent);
            }
        }).start();
    }

    /*YUNOS BEGIN*/
    //##module(HomeShell)
    //##date:2014/04/23##author:hao.liuhaolh@alibaba-inc.com##BugID:110668
    //check restore data to avoid no item in dock
    private boolean isRestoreDataError(SQLiteDatabase db) {
        Log.d(TAG, "isRestoreDataError in");
        boolean ret = false;
        Cursor c = db.query("favorites", null, "screen=? and container=?",
                                     new String[] { String.valueOf(0), String.valueOf(-100)}, null, null, null);
        if (c != null) {
            Log.d(TAG, "the first screen item count is " + c.getCount());
        }
        //in restore data, the first screen shouldn't empty
        if ((c == null) || (c.getCount() <= 0)) {
            ret = true;
        }

        if (c != null) {
            c.close();
        }
        Log.d(TAG, "isRestoreDataError out");
        return ret;
    }
    /*YUNOS END*/

    /*YUNOS BEGIN*/
    //##module(HomeShell)
    //##date:2014/03/28 ##author:nater.wg ##BugID:105796
    //update restore database for system applications
    private void updateRestoreDb(SQLiteDatabase db) {
        updateActionsFrom2_0To2_5(db);
        updateActionsFrom2_1To2_5(db);
        updateActionsFrom2_3To2_5_updateDockItems(db);
        updateActionsFrom2_5To2_7(db);
        updateActionsFrom2_7To3_0_updateGadgetItems(db);
        updateActionsFrom2_7To3_0(db);
    }

    private void mappingOldPkgToNew(String intentStr, String conditionStr, SQLiteDatabase db){
        String sql = "UPDATE favorites SET intent='" +
                intentStr +
                "' WHERE intent like '" +
                conditionStr +
                "'";
        db.execSQL(sql);
    }

    private void updateActionsFrom2_0To2_5(SQLiteDatabase db) {
        Log.d(TAG, "updateActionsFrom2_0To2_5");

//        String sql;
        // Dialtcts application
        String intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;" +
                "launchFlags=0x10200000;component=com.yunos.alicontacts/.activities.DialtactsActivity;end";
        String conditionStr = "%com.android.contacts/.TwelveKeyDialer%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        // message
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.alicontacts/com.yunos.alimms.ui.ConversationList;end";
        conditionStr = "%com.android.mms/.ui.ConversationList%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        // contacts
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.alicontacts/.activities.PeopleActivity2;end";
        conditionStr = "%com.android.contacts/.DialtactsContactsEntryActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        // browser
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.UCMobile.yunos/com.UCMobile.main.UCMobile;end";
        conditionStr = "%com.aliyun.mobile.browser%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        Log.d(TAG, "update browser");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.UCMobile.yunos/com.UCMobile.main.UCMobile;end";
        conditionStr = "%com.android.browser%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        Log.d(TAG, "update image");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                 "component=com.aliyun.image/.app.Gallery;end";
        conditionStr = "%com.aliyun.image%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
        /*YUNOS END*/

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.duomi.yunos/.DMLauncher;end";
        conditionStr = "%com.aliyun.music/.MusicTabActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.SecurityCenter/.ui.SecurityCenterActivity;end";
        conditionStr = "%com.aliyun.SecurityCenter/.MainActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.mobile.email/.activity.Welcome;end";
        conditionStr = "%com.aliyun.mobile.email/.activity.Email%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.android.calendar/.AllInOneActivity;end";
        conditionStr = "%com.android.calendar/.LaunchActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=xcxin.filexpertaliyun/.FileLister;end";
        conditionStr = "%com.aliyun.filemanager/.FileListActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.theme.thememanager/.ThemeManagerSlideActivity;end";
        conditionStr = "%=com.aliyun.homeshell/com.yunos.theme.thememanager.ThemeManagerSlideActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
    }

    private void updateActionsFrom2_1To2_5(SQLiteDatabase db) {
        Log.d(TAG,"updateActionsFrom2_1To2_5");
        String intentStr, conditionStr;

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.image/.app.Gallery;end";
        conditionStr = "%com.aliyun.image/com.aliyun.gallery.GalleryShortCutActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
        /* YUNOS BEGIN */
        //##date:2013/12/4 ##author:hongxing.whx ##bugid: 70388
        //security center app
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.SecurityCenter/.ui.SecurityCenterActivity;end";
        conditionStr = "%com.aliyun.SecurityCenter/.ui.SplashActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
        //theme center
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.theme.thememanager/.ThemeManagerSlideActivity;end";
        conditionStr = "%=com.aliyun.homeshell/com.yunos.theme.thememanager.ThemeManagerSlideActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
        /* YUNOS END */

        // browser
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.UCMobile.yunos/com.UCMobile.main.UCMobile;end";
        conditionStr = "%com.android.browser/.BrowserActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
    }

    private void updateActionsFrom2_3To2_5_updateDockItems(SQLiteDatabase db) {
        Log.d(TAG,"updateActionsFrom2_3To2_5_updateDockItems");
        try {
            String updateSQL = "UPDATE favorites SET screen=? WHERE _id=?";
            String sql = "select * from favorites where container = '-101' ";
            Log.d(TAG, "updateDockItems");
            Cursor cursor = db.rawQuery(sql, null);
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    int[] indexes = new int[3];
                    int cellYIndex = cursor.getColumnIndexOrThrow(Favorites.CELLY);
                    int cellXIndex = cursor.getColumnIndexOrThrow(Favorites.CELLX);
                    int idIndex = cursor.getColumnIndexOrThrow(Favorites._ID);
                    int screenIndex = cursor.getColumnIndex(Favorites.SCREEN);
                    indexes[0] = cursor.getInt(screenIndex);
                    indexes[1] = cursor.getInt(cellXIndex);
                    indexes[2] = cursor.getInt(cellYIndex);
                    int id = cursor.getInt(idIndex);
                    db.execSQL(updateSQL, new String[]{String.valueOf(indexes[1]), String.valueOf(id)});
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed in updateDockItems : " + e.getMessage());
        }
    }

    private void updateActionsFrom2_5To2_7(SQLiteDatabase db) {
        Log.d(TAG,"updateActionsFrom2_5To2_7");
        String intentStr, conditionStr;
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.camera/.CameraActivity;end";
        conditionStr = "%com.android.gallery3d/com.android.camera.CameraLauncher%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.filemanager/com.aliyun.filemanager.FileListActivity;end";
        conditionStr = "%xcxin.filexpertaliyun/.FileLister%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
    }

    private void updateActionsFrom2_7To3_0_updateGadgetItems(SQLiteDatabase db) {
        Log.d(TAG, "updateActionsFrom2_7To3_0_updateGadgetItems");
        try {
            String sql = "UPDATE favorites SET itemType = '10', title = 'clock_4x1' WHERE itemType=7";
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e(TAG, "Failed in updateBookmarkItems : " + e.getMessage());
        }
    }

    private void updateActionsFrom2_7To3_0(SQLiteDatabase db) {
        Log.d(TAG, "updateActionsFrom2_7To3_0");
        String intentStr;
        String conditionStr;

        //AlarmClock
        Log.d(TAG, "alarm clock convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.android.deskclock/.DeskClock;end";
        conditionStr = "%com.android.alarmclock/.AlarmClock%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //FileManager 2.7 to 3.0
        Log.d(TAG, "FileManager convert from 2.7 to 3.0");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyunos.filemanager/.FileManagerAppFrame;end";
        conditionStr = "%com.aliyun.filemanager/com.aliyun.filemanager.FileListActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //FileManager 2.7.1 to 3.0
        Log.d(TAG, "FileManager convert from 2.7.1 to 3.0");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyunos.filemanager/.FileManagerAppFrame;end";
        conditionStr = "%com.aliyun.filemanager/.FileListActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //Calculator
        Log.d(TAG, "Calculator convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.calculator/.Calculator;end";
        conditionStr = "%com.android.calculator2/.Calculator%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //FMRadio
        Log.d(TAG, "FMRadio convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.FMRadio/.FMRadioActivity;end";
        conditionStr = "%com.mediatek.FMRadio/.FMRadioActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //DialtactsContacts
        Log.d(TAG, "DialtactsContacts convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.yunos.alicontacts/.activities.DialtactsContactsActivity;end";
        conditionStr = "%com.yunos.alicontacts/.activities.DialtactsActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //mms
        Log.d(TAG, "mms convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.android.mms/.ui.ConversationList;end";
        conditionStr = "%com.yunos.alicontacts/com.yunos.alimms.ui.ConversationList%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //note
        Log.d(TAG, "note convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.aliyun.note/.activity.NotesListActivity;end";
        conditionStr = "%com.aliyun.note/.NoteActivity%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //setting
        Log.d(TAG, "setting convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.android.settings/.Settings;end";
        conditionStr = "%com.android.settings/.aliyun.AliSettingsMain%";
        mappingOldPkgToNew(intentStr, conditionStr, db);

        //gaodi map
        Log.d(TAG, "setting convert");
        intentStr = "#Intent;action=android.intent.action.MAIN;" +
                "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                "component=com.autonavi.minimap/com.autonavi.map.activity.SplashActivity;end";
        conditionStr = "%com.autonavi.minimap%";
        mappingOldPkgToNew(intentStr, conditionStr, db);
    }
    /*YUNOS END*/
}
