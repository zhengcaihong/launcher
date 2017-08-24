package com.aliyun.homeshell.smartlocate;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class defines, manages and encapsulates SQL operations for app-launch information.<p>
 * All the data is saved in SQL table "app_launch" of "homeshell.db" database.
 * @see http://docs.alibaba-inc.com:8090/pages/viewpage.action?pageId=258623401
 * @author zhanggong.zg
 */
public final class AppLaunchDBHelper {

    public static final String SQL_CREATE_APP_LAUNCH_TABLE =
              "CREATE TABLE app_launch ("
            + "  _id  integer NOT NULL,"
            + "  time integer NOT NULL,"
            + "  cnt  float NOT NULL DEFAULT(1)"
            + ")";

    public static final String SQL_CREATE_APP_LAUNCH_INDEX =
            "CREATE UNIQUE INDEX idx ON app_launch (_id, time)";

    public static final String SQL_DROP_APP_LAUNCH_TABLE =
            "DROP TABLE IF EXISTS app_launch";
    private static final String TAG = "AppLaunchDBHelper";
    private static final String TABLE_NAME = "app_launch";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TIME = "time";
    private static final String COLUMN_COUNT = "cnt";
    private static final String[] COLUMNS = { COLUMN_ID, COLUMN_TIME, COLUMN_COUNT };
    private static final String WHERE_CLAUSE_ID_TIME = COLUMN_ID + "=? AND " + COLUMN_TIME + "=?";
    private static final String WHERE_CLAUSE_ID = COLUMN_ID + "=?";

    interface RecordVisitor {
        void visitRecord(long id, int time, float count);
    }

    //// Instance Members ////

    private SQLiteDatabase mDB;

    AppLaunchDBHelper(SQLiteOpenHelper helper) {
        try{
            mDB = helper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in removeFromDb : " + e.getMessage());
            mDB = null;
        }
    }

    /**
     * Traverses all records in table.
     */
    void traverse(RecordVisitor visitor) {
        if (visitor == null || mDB == null) return;
        Cursor cursor = null;
        try {
            cursor = mDB.query(TABLE_NAME, COLUMNS, null, null, null, null, null);
            int col_id = cursor.getColumnIndex(COLUMN_ID);
            int col_time = cursor.getColumnIndex(COLUMN_TIME);
            int col_count = cursor.getColumnIndex(COLUMN_COUNT);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(col_id);
                int time = cursor.getInt(col_time);
                float count = cursor.getFloat(col_count);
                visitor.visitRecord(id, time, count);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Insert a new record into table.
     */
    void insert(long id, int time, float count) {
        if(null == mDB){
            Log.e(TAG,"insert error for mDB is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_TIME, time);
        values.put(COLUMN_COUNT, count);
        mDB.insert(TABLE_NAME, null, values);
    }

    /**
     * Updates count for the corresponding record of given id and time.
     */
    void update(long id, int time, float newCount) {
        if(null == mDB){
            Log.e(TAG,"update error for mDB is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_COUNT, newCount);
        String[] args = { String.valueOf(id), String.valueOf(time) };
        mDB.update(TABLE_NAME, values, WHERE_CLAUSE_ID_TIME, args);
    }

    /**
     * Deletes the record of given id and time.
     */
    void delete(long id, int time) {
        if(null == mDB){
            Log.e(TAG,"delete error for mDB is null");
            return;
        }
        String[] args = { String.valueOf(id), String.valueOf(time) };
        mDB.delete(TABLE_NAME, WHERE_CLAUSE_ID_TIME, args);
    }

    /**
     * Deletes all records of given id.
     */
    void delete(long id) {
        if(null == mDB){
            Log.e(TAG,"delete error for mDB is null");
            return;
        }
        mDB.delete(TABLE_NAME, WHERE_CLAUSE_ID, new String[] { String.valueOf(id) });
    }

    /**
     * Deletes the record of given id and time.
     */
    void deleteAll() {
        if(null == mDB){
            Log.e(TAG,"deleteAll error for mDB is null");
            return;
        }
        mDB.delete(TABLE_NAME, null, null);
    }

    void transaction(Runnable task) {
        if(null == mDB){
            Log.e(TAG,"transaction error for mDB is null");
            return;
        }
        if (task == null) return;
        mDB.beginTransaction();
        try {
            task.run();
            mDB.setTransactionSuccessful();
        } finally {
            mDB.endTransaction();
        }
    }

}
