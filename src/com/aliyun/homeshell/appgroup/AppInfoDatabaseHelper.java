package com.aliyun.homeshell.appgroup;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppInfoDatabaseHelper extends SQLiteOpenHelper {
    final static int DB_VERSION = 1;
    public final static String DB_NAME = "applications.db";
    public static final String DB_DEFAULT_APPINFO = "default_appInfo.db";
    final static String TABLE_APP_NAME = "appInfo";
    final static String TABLE_CAT_NAME = "categoryInfo";
    final static String COLUMN_PKG_NAME = "pkgName";
    final static String COLUMN_CATEGORY_ID = "catId";
    final static String COLUMN_ORIGINAL_NAME = "originalName";
    final static String COLUMN_SHOW_NAME = "showName";
    final static String COLUMN_SUPER_ID = "superId";
    final static String COLUMN_SUPER_NAME = "superName";

    public AppInfoDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_APP_NAME + " (" + COLUMN_PKG_NAME
                + " TEXT NOT NULL, " + COLUMN_CATEGORY_ID + " TEXT NOT NULL, " + "PRIMARY KEY ("
                + COLUMN_PKG_NAME + ") " + ");");
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CAT_NAME + " (" + COLUMN_CATEGORY_ID
                + " TEXT NOT NULL, " + COLUMN_ORIGINAL_NAME + " TEXT NOT NULL, " + COLUMN_SHOW_NAME
                + " TEXT, " + COLUMN_SUPER_ID + " TEXT, " + COLUMN_SUPER_NAME + " TEXT, "
                + "PRIMARY KEY (" + COLUMN_CATEGORY_ID + ") " + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DELETE FROM " + TABLE_APP_NAME);
            db.execSQL("DELETE FROM " + TABLE_CAT_NAME);
            onCreate(db);
        }
    }

}
