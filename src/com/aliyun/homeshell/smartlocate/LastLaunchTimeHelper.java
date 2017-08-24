package com.aliyun.homeshell.smartlocate;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.utils.Utils;

import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.LongSparseArray;

/**
 * This class stores the last-launch time for each app. The data is stored
 * in a new SQL table called "last_launch_time" in homeshell database.<p>
 * When homeshell starts up, all the data is cached into a map (see {@link #initialize()}).
 * When an app is launched, the last-launch time will be updated both in cache and
 * database (see {@link #updateLastLaunchTime(long)}.
 * @author zhanggong.zg
 */
public final class LastLaunchTimeHelper {

    private static final String TAG = "LastLaunchTimeHelper";

    public static final String SQL_CREATE_LAST_LAUNCH_TABLE =
              "CREATE TABLE last_launch_time ("
            + "  _id  integer NOT NULL,"
            + "  dateTime integer NOT NULL DEFAULT(0)"
            + ")";

    public static final String SQL_DROP_LAST_LAUNCH_TABLE =
            "DROP TABLE IF EXISTS last_launch_time";

    private static final String TABLE_NAME = "last_launch_time";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DATETIME = "dateTime";
    private static final String[] COLUMNS = { COLUMN_ID, COLUMN_DATETIME, };
    private static final String WHERE_CLAUSE_ID = COLUMN_ID + "=?";

    //// Instance Members ////

    private SQLiteDatabase mDB;
    private final Map<Long, Long> mCache; // id -> unix time

    LastLaunchTimeHelper(SQLiteOpenHelper helper) {
        try{
            mDB = helper.getWritableDatabase();
        }catch (SQLiteException e) {
            Log.e(TAG, "Failed in removeFromDb : " + e.getMessage());
            mDB = null;
        }
        mCache = new ConcurrentHashMap<Long, Long>();
    }

    public void sortItemsByLastLaunchTime(List<? extends ShortcutInfo> items) {
        Log.d(TAG, "sortItemsByLastLaunchTime in: " + items.size());
        Collections.sort(items, mComparator);
        Log.d(TAG, "sortItemsByLastLaunchTime out");
    }

    private final Comparator<ShortcutInfo> mComparator = new Comparator<ShortcutInfo>() {

        private PackageManager packageManager;
        private LongSparseArray<Long> installTimeMap;

        @Override
        public int compare(ShortcutInfo item1, ShortcutInfo item2) {
            Long time1 = getLastLaunchOrInstallTime(item1);
            Long time2 = getLastLaunchOrInstallTime(item2);
            if (time1 != null && time2 != null) {
                long dt = time2.longValue() - time1.longValue();
                return dt > 0 ? 1 : (dt < 0 ? -1 : 0);
            } else if (time1 != null && time2 == null) {
                return -1;
            } else if (time1 == null && time2 != null) {
                return 1;
            } else {
                return (int) (item1.id - item2.id);
            }
        }

        private Long getLastLaunchOrInstallTime(ShortcutInfo item) {
            Long time = mCache.get(item.id);
            if (time != null) {
                return time;
            } else if (installTimeMap != null &&
                       installTimeMap.indexOfKey(item.id) > 0) {
                return installTimeMap.get(item.id);
            } else {
                // get installation time
                try {
                    if (packageManager == null) {
                        packageManager = LauncherApplication.getContext().getPackageManager();
                    }
                    PackageInfo packageInfo = packageManager.getPackageInfo(item.getPackageName(), 0);
                    if (installTimeMap == null) {
                        installTimeMap = new LongSparseArray<Long>();
                    }
                    installTimeMap.put(item.id, packageInfo.firstInstallTime);
                    return packageInfo.firstInstallTime;
                } catch (NameNotFoundException e) {
                    return null;
                }
            }
        }

    };

    void initialize() {
        Log.d(TAG, "initialize in");
        if(null == mDB){
            Log.e(TAG,"initialize error for mDB is null");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = mDB.query(TABLE_NAME, COLUMNS, null, null, null, null, null);
            int col_id = cursor.getColumnIndex(COLUMN_ID);
            int col_time = cursor.getColumnIndex(COLUMN_DATETIME);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(col_id);
                long time = cursor.getLong(col_time);
                mCache.put(id, time);
            }
            Log.d(TAG, "initialize load to cache: " + mCache.size());
        } catch (SQLiteException ex) {
            Log.e(TAG, "initialize sql error", ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "initialize out");
    }

    void updateLastLaunchTime(long id) {
        Log.d(TAG, "updateLastLaunchTime in");
        long dateTime = Calendar.getInstance().getTimeInMillis();
        try {
            if (mCache.containsKey(id)) {
                // update
                mCache.put(id, dateTime);
                update(id, dateTime);
            } else {
                // insert
                mCache.put(id, dateTime);
                insert(id, dateTime);
            }
        } catch (SQLiteFullException e) {
            Log.e(TAG, "updateLastLaunchTime sql error DB is full", e);
            //BugID:6638738:no throw the full exception to avoid homeshell crash
            //throw e;
        } catch (SQLiteException ex) {
            Log.e(TAG, "updateLastLaunchTime sql error", ex);
        }
        Log.d(TAG, "updateLastLaunchTime out");
    }

    void removeLastLaunchTime(long id) {
        Log.d(TAG, "removeLastLaunchTime in");
        if (mCache.containsKey(id)) {
            try {
                mCache.remove(id);
                delete(id);
            } catch (SQLiteException ex) {
                Log.e(TAG, "removeLastLaunchTime sql error", ex);
            }
        }
        Log.d(TAG, "removeLastLaunchTime out");
    }

    private void insert(long id, long dateTime) {
        if(null == mDB){
            Log.e(TAG,"insert error for mDB is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_DATETIME, dateTime);
        mDB.insert(TABLE_NAME, null, values);
    }

    private void update(long id, long dateTime) {
        if(null == mDB){
            Log.e(TAG,"update error for mDB is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATETIME, dateTime);
        mDB.update(TABLE_NAME, values, WHERE_CLAUSE_ID, new String[] { String.valueOf(id) });
    }

    private void delete(long id) {
        if(null == mDB){
            Log.e(TAG,"delete error for mDB is null");
            return;
        }
        mDB.delete(TABLE_NAME, WHERE_CLAUSE_ID, new String[] { String.valueOf(id) });
    }

    @SuppressWarnings("unused")
    private void deleteAll() {
        if(null == mDB){
            Log.e(TAG,"deleteAll error for mDB is null");
            return;
        }
        mDB.delete(TABLE_NAME, null, null);
    }

}
