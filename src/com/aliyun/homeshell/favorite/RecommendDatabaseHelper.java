package com.aliyun.homeshell.favorite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class RecommendDatabaseHelper extends SQLiteOpenHelper {

    static final String APP_RECORDE_TABLE = "app_recoder";
    static final String APP_ID = "appId";
    static final String DATE = "date";
    static final String CLICK_COUNT = "clickCount";
    static final String CLICK_TIME = "clickTime";

    private static final String DATABASE_NAME= "apprecord.db";
    private static final int VERSION = 1000;

    public RecommendDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("create table if not exists "
        + APP_RECORDE_TABLE +
        "(id integer primary key," +
        "appId integer," +
        "date integer," +
        "clickCount integer," +
        "clickTime integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}
