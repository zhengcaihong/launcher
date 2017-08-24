package com.aliyun.homeshell.iconupdate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IconUpdateDBHelper extends SQLiteOpenHelper {
    final static int DB_VERSION = 1;
    public final static String DB_NAME = "iconupdate.db";
    final static String TABLE_ICONUPDATE = "iconupdate";

    public static final String ID = "_id";
    public static final String COMPONENTNAME = "componentName";
    public static final String ICON = "icon";
    public static final String STARTTIME = "startTime";
    public static final String ENDTIME = "endTime";

    public IconUpdateDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_ICONUPDATE + " ("
                + COMPONENTNAME + " TEXT PRIMARY KEY," + ICON + " BLOB,"
                + STARTTIME + " TEXT," + ENDTIME + " TEXT" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DELETE FROM " + TABLE_ICONUPDATE);
            onCreate(db);
        }
    }

}
