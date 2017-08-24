/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyun.homeshell;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.smartlocate.AppLaunchDBHelper;
import com.aliyun.homeshell.smartlocate.AppLaunchManager;
import com.aliyun.homeshell.smartlocate.AppLaunchUpdater;
import com.aliyun.homeshell.smartlocate.LastLaunchTimeHelper;
import com.aliyun.homeshell.smartsearch.HanziToPinyin;
import com.aliyun.homeshell.utils.Utils;

/* YUNOS BEGIN CMCC */
//author:xianqiu.zbb
//BugID:(8716966) ##date:2016.8.15
//descrpition: 修改移动入库新的应用预置需求
import android.os.SystemProperties;
/* YUNOS END CMCC */

public class LauncherProvider extends ContentProvider {
    private static final String TAG = "Launcher.LauncherProvider";
    private static final boolean LOGD = true;            //YUNOS hao.liuhaolh change to true for debug

    public static final String DATABASE_NAME = "homeshell.db";
    public static long agedDefaultFolderId = -1l;
    public static ScreenPosition sMaxPosAfterLoadFav = new ScreenPosition(-1, -1, -1);
    //2000: codebase 2.5
    //2001: codebase 2.7

    /*YUNOS BEGIN*/
    //##date:2014/03/15 ##author:hao.liuhaolh ##BugID:101011
    //handle gadget type in restore and fota upgrade
    //3000: codebase 3.0
    //3010: codebase 3.0 with smart search
    //3020: codebase 3.0 with aged mode
    //3030: codebase 3.0 with favorite app
    //3035: gaode map changed from custom to public version, package name changed
    //3040: huanji package name changed
    //3050: aged mode
    //3055: for adding search widget
    //3060: pad land mode
    // 3062:smart locate
    // 3063:support Atom Icon, and make dock max count is 4
    // 3064:support multi-profile
    private static final int DATABASE_VERSION = 3064;
    /*YUNOS END*/

    static final String AUTHORITY = "com.aliyun.homeshell.settings";

    static final String TABLE_FAVORITES = "favorites";
    static final String NORMAL_TABLE_FAVORITES = "normal_favorites";
    static final String PARAMETER_NOTIFY = "notify";
    static final String DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED =
            "DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED";
    static final String DEFAULT_WORKSPACE_RESOURCE_ID =
            "DEFAULT_WORKSPACE_RESOURCE_ID";

    /* YUNOS BEGIN */
    // ##date:2015/9/1 ##author:zhanggong.zg ##BugID:6384589
    // used to support smart app suggestion (since version 3061)
    public static final String DATABASE_CREATION_TIME = "DATABASE_CREATION_TIME";
    /* YUNOS END */

    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    static final String TABLE_AGED_FAVORITES = "aged_favorites";
    static final String DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED =
            "DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED";
    static final String DB_AGED_MODE_FLAG = "DB_AGED_MODE_FLAG";
    public static final int SETTINGS_AGED_MODE = 1;
    /*YUNOS END*/

    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE =
            "com.aliyun.homeshell.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";

    /**
     * {@link Uri} triggered at any registered {@link android.database.ContentObserver} when
     * {@link AppWidgetHost#deleteHost()} is called during database creation.
     * Use this to recall {@link AppWidgetHost#startListening()} if needed.
     */
    static final Uri CONTENT_APPWIDGET_RESET_URI =
            Uri.parse("content://" + AUTHORITY + "/appWidgetReset");

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {

        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
//        String spKey = LauncherApplication.getSharedPreferencesKey();
        mDbAgedMode = (SETTINGS_AGED_MODE == Settings.Secure.getInt(
                getContext().getContentResolver(), "aged_mode", 0));
        /*YUNOS END*/

        mOpenHelper = new DatabaseHelper(getContext());
        ((LauncherApplication) getContext()).setLauncherProvider(this);
        AppLaunchManager.init(mOpenHelper);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(args.table);
    
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
            result.setNotificationUri(getContext().getContentResolver(), uri);
    
            return result;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in query : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in query : " + e.getMessage());
        }

        return null;
    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (!values.containsKey(LauncherSettings.Favorites._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        
        try {
            return db.insert(table, nullColumnHack, values);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in dbInsertAndCheck : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in dbInsertAndCheck : " + e.getMessage());
        }

        return -1;
    }

    private static void deleteId(SQLiteDatabase db, long id) {
        try {
            Uri uri = LauncherSettings.Favorites.getContentUri(id, false);
            SqlArguments args = new SqlArguments(uri, null, null);
            db.delete(args.table, args.where, args.args);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in deleteId : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in deleteId : " + e.getMessage());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        try {
            SqlArguments args = new SqlArguments(uri);
    
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
            if (rowId <= 0) return null;
    
            uri = ContentUris.withAppendedId(uri, rowId);
            sendNotify(uri);
    
            return uri;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in insert : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in insert : " + e.getMessage());
        }
        
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SQLiteDatabase db = null;

        try {
            SqlArguments args = new SqlArguments(uri);
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in bulkInsert : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in insert : " + e.getMessage());
        } finally {
            if (db != null) db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
    
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            int count = db.delete(args.table, args.where, args.args);
            if (count > 0) sendNotify(uri);
            return count;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in delete : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e ) {
            Log.e(TAG, "Failed in delete : " + e.getMessage());
        }
        
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
    
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            int count = db.update(args.table, values, args.where, args.args);
            if (count > 0) sendNotify(uri);
    
            return count;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in update : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e ) {
            Log.e(TAG, "Failed in update : " + e.getMessage());
        }
        
        return 0;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    /*YUNOS BEGIN*/
    //##date:2014/03/11 ##author:hao.liuhaolh ##BugID: 99159
    //re-array screen database update issue during power off
    public static class updateArgs {
        public Uri mUri;
        public ContentValues mValues;
        public String mSelection;
        public String[] mSelectionArgs;
        public updateArgs(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            mUri = uri;
            mValues = values;
            mSelection = selection;
            mSelectionArgs = selectionArgs;
        }
    }

    public int bulkUpdate(ArrayList<updateArgs> updatelist) {
        int ret = 0;
        SQLiteDatabase db = null;

        try {
            Log.d(TAG, "bulkupdate in");
            db = mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            int numValues = updatelist.size();
            for (int i = 0; i < numValues; i++) {
                SqlArguments args = new SqlArguments(updatelist.get(i).mUri,
                                                     updatelist.get(i).mSelection,
                                                     updatelist.get(i).mSelectionArgs);
                ret += db.update(args.table, updatelist.get(i).mValues, args.where, args.args);
            }
            db.setTransactionSuccessful();
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in bulkUpdate : throw SQLiteFullException");
            if(!Utils.isLowSpace()){
                throw e;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in bulk update : " + e.getMessage());
            ret = 0;
        } finally {
            if (db != null) db.endTransaction();
        }
        return ret;
    }
    /*YUNOS END*/

    public long generateNewId() {
        return mOpenHelper.generateNewId();
    }

    /**
     * @param workspaceResId that can be 0 to use default or non-zero for specific resource
     */
    synchronized public void loadDefaultFavoritesIfNecessary(int origWorkspaceResId) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);
        if (!mDbAgedMode && sp.getBoolean(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED, false)) {
            int workspaceResId = origWorkspaceResId;

            // Use default workspace resource if none provided
            if (workspaceResId == 0) {
                if(Utils.isForLightUpdate()){
                    workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace_lightupdate);
                }else if (Utils.isForCMCC()) {
                    Log.d(TAG, "build for cmcc");
                    /* YUNOS BEGIN CMCC */
                    //author:xianqiu.zbb
                    //BugID:(8716966) ##date:2016.8.15
                    //descrpition: 修改移动入库新的应用预置需求
                    if (SystemProperties.getBoolean("ro.yunos.cmcc.newreq", false)) {
                        workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace_for_cmcc_new);
                    } else {
                        workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace_for_cmcc);
                    }
                } else if(android.os.Build.YUNOS_CARRIER_CUCC){
                    Log.d(TAG, "build for cucc");
                    workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace_for_cucc);
                } else {
                    if (ConfigManager.isLandOrienSupport()) {
                        Log.d(TAG, "sxsexe_pad     load default_workspace_landsupport.xml  ");
                        workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace_landsupport);
                    } else {
                        workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace);
                    }
                }
            }

            // Populate favorites table with initial favorites
            SharedPreferences.Editor editor = sp.edit();
            editor.remove(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED);
            if (origWorkspaceResId != 0) {
                editor.putInt(DEFAULT_WORKSPACE_RESOURCE_ID, origWorkspaceResId);
            }
            mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase(), workspaceResId);
            editor.commit();

            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
        } else if (mDbAgedMode && sp.getBoolean(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED, false)) {
            SharedPreferences.Editor editor = sp.edit();
            editor.remove(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED);
            mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase(), R.xml.aged_workspace);
            editor.commit();
            /*YUNOS END*/
        } else {
            return;
        }
        mOpenHelper.syncMessageNumbers();
        mOpenHelper.syncHideseat();
    }

    /*YUNOS BEGIN*/
    //##date:2014/8/8 ##author:zhangqiang.zq
    // favorite apps
    public void clearFavoriteIcons() {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL("UPDATE favorites SET favoriteIcon=NULL");
    }
    /*YUNOS END*/
    /*YUNOS BEGIN*/
    //##date:2014/7/8 ##author:zhangqiang.zq
    // aged mode
    public void switchDB(boolean dbAgedMode) {
        mDbAgedMode = dbAgedMode;
        mOpenHelper.setAgedMode(dbAgedMode);
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);
        if ((!mDbAgedMode || !sp.getBoolean(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED, false))
                &&
                (mDbAgedMode || !sp.getBoolean(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED,
                        false))) {
            mOpenHelper.syncMessageNumbers();
            mOpenHelper.syncHideseat();
        }
    }

    private static boolean mDbAgedMode;
    /*YUNOS END*/
    public static boolean getDbAgedModeState() {
        return mDbAgedMode;
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG_FAVORITES = "favorites";
        private static final String TAG_FAVORITE = "favorite";
        private static final String TAG_CLOCK = "clock";
        private static final String TAG_SEARCH = "search";
        private static final String TAG_APPWIDGET = "appwidget";
        private static final String TAG_SHORTCUT = "shortcut";
        private static final String TAG_FOLDER = "folder";
        private static final String TAG_EXTRA = "extra";
        private static final String TAG_ALIAPPWIDGET = "aliappwidget";
        private static final String TAG_GADGET = "gadget";

        private final Context mContext;
        private final AppWidgetHost mAppWidgetHost;
        private long mMaxId = -1;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
            mAppWidgetHost = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);

            // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
            // the DB here
            if (mMaxId == -1) {
                mMaxId = initializeMaxId(getWritableDatabase());
            }
        }

        /**
         * Send notification that we've deleted the {@link AppWidgetHost},
         * probably as part of the initial database creation. The receiver may
         * want to re-call {@link AppWidgetHost#startListening()} to ensure
         * callbacks are correctly set.
         */
        private void sendAppWidgetResetNotify() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD) Log.d(TAG, "creating new launcher database");

            mMaxId = 1;

            Log.d(TAG_APPWIDGET, "sxsexe_pad    onCreate create table favorites     ");

            db.execSQL("CREATE TABLE favorites (" +
                    "_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "intent TEXT," +
                    "container INTEGER," +
                    "screen INTEGER," +
                    "cellX INTEGER," +
                    "cellY INTEGER," +
                    "cellXLand INTEGER," +
                    "cellYLand INTEGER," +
                    "spanX INTEGER," +
                    "spanY INTEGER," +
                    "itemType INTEGER," +
                    "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                    "isShortcut INTEGER DEFAULT 0," +
                    "iconType INTEGER," +
                    "iconPackage TEXT," +
                    "iconResource TEXT," +
                    "icon BLOB," +
                    "uri TEXT," +
                    "displayMode INTEGER," +
                    "canDelete INTEGER DEFAULT 1," +
                    "messageNum INTEGER DEFAULT 0," +
                    "isNew INTEGER DEFAULT 0," +
                    /*YUNOS BEGIN*/
                  //##date:2014/6/4 ##author:zhangqiang.zq
                  // smart search
                    "fullPinyin TEXT," +
                    "shortPinyin TEXT," +
                    /*YUNOS END*/
                    /*YUNOS BEGIN*/
                    //##date:2014/6/4 ##author:zhangqiang.zq
                    // favorite apps
                      "favoriteWeight INTEGER DEFAULT 0," +
                      "favoriteIcon BLOB, " +
                      /*YUNOS END*/
                      "userId INTEGER" +
                    ");");
            /*YUNOS hao.liuhaolh add three items at the end of CREATE TABLE*/

            /*YUNOS BEGIN*/
            //##date:2014/6/4 ##author:zhangqiang.zq
            // smart search
            db.execSQL("DROP TABLE IF EXISTS aged_favorites");
            db.execSQL("CREATE TABLE aged_favorites (" +
                    "_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "intent TEXT," +
                    "container INTEGER," +
                    "screen INTEGER," +
                    "cellX INTEGER," +
                    "cellY INTEGER," +
                    "cellXLand INTEGER," +
                    "cellYLand INTEGER," +
                    "spanX INTEGER," +
                    "spanY INTEGER," +
                    "itemType INTEGER," +
                    "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                    "isShortcut INTEGER DEFAULT 0," +
                    "iconType INTEGER," +
                    "iconPackage TEXT," +
                    "iconResource TEXT," +
                    "icon BLOB," +
                    "uri TEXT," +
                    "displayMode INTEGER," +
                    "canDelete INTEGER DEFAULT 1," +
                    "messageNum INTEGER DEFAULT 0," +
                    "isNew INTEGER DEFAULT 0," +
                    "fullPinyin TEXT," +
                    "shortPinyin TEXT," +
                    "favoriteWeight INTEGER DEFAULT 0," +
                    "favoriteIcon BLOB, " +
                    "userId INTEGER" +
                    ");");
            /*YUNOS END*/

            /* YUNOS BEGIN */
            // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6348948
            // Smart app recommendation (to record app launch times and counts)
            db.execSQL(AppLaunchDBHelper.SQL_DROP_APP_LAUNCH_TABLE);
            db.execSQL(AppLaunchDBHelper.SQL_CREATE_APP_LAUNCH_TABLE);
            db.execSQL(LastLaunchTimeHelper.SQL_DROP_LAST_LAUNCH_TABLE);
            db.execSQL(LastLaunchTimeHelper.SQL_CREATE_LAST_LAUNCH_TABLE);
            AppLaunchUpdater.setupAlarm(mContext);
            /* YUNOS END */

            // Database was just created, so wipe any previous widgets
            if (mAppWidgetHost != null) {
                mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }

            if (!convertDatabase(db)) {
                // Set a shared pref so that we know we need to load the default workspace later
                if (LOGD) Log.d(TAG, "set flag to load default workspace later");            //YUNOS
                setFlagToLoadDefaultWorkspaceLater();
            }
        }

        private void setFlagToLoadDefaultWorkspaceLater() {
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();

            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
            editor.putBoolean(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED, true);
            /*YUNOS END*/

            editor.putBoolean(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED, true);
            editor.commit();
        }

        private boolean convertDatabase(SQLiteDatabase db) {
            if (LOGD) Log.d(TAG, "converting database from an older format, but not onUpgrade");
            boolean converted = false;

            final Uri uri = Uri.parse("content://" + Settings.AUTHORITY +
                    "/old_favorites?notify=true");
            final ContentResolver resolver = mContext.getContentResolver();
            Cursor cursor = null;

            try {
                cursor = resolver.query(uri, null, null, null, null);
            } catch (Exception e) {
                // Ignore
            }

            // We already have a favorites database in the old provider
            if (cursor != null && cursor.getCount() > 0) {
                try {
                    converted = copyFromCursor(db, cursor) > 0;
                } finally {
                    cursor.close();
                }

                if (converted) {
                    resolver.delete(uri, null, null);
                }
            }

            if (converted) {
                // Convert widgets from this import into widgets
                if (LOGD) Log.d(TAG, "converted and now triggering widget upgrade");
                convertWidgets(db);
            }

            return converted;
        }

        private int copyFromCursor(SQLiteDatabase db, Cursor c) {
            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
            final int iconTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
            final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
            final int iconPackageIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
            final int iconResourceIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
            final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
            final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
            final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
            final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
            final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
            final int displayModeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.DISPLAY_MODE);

            ContentValues[] rows = new ContentValues[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                ContentValues values = new ContentValues(c.getColumnCount());
                values.put(LauncherSettings.Favorites._ID, c.getLong(idIndex));
                values.put(LauncherSettings.Favorites.INTENT, c.getString(intentIndex));
                values.put(LauncherSettings.Favorites.TITLE, c.getString(titleIndex));
                values.put(LauncherSettings.Favorites.ICON_TYPE, c.getInt(iconTypeIndex));
                values.put(LauncherSettings.Favorites.ICON, c.getBlob(iconIndex));
                values.put(LauncherSettings.Favorites.ICON_PACKAGE, c.getString(iconPackageIndex));
                values.put(LauncherSettings.Favorites.ICON_RESOURCE, c.getString(iconResourceIndex));
                values.put(LauncherSettings.Favorites.CONTAINER, c.getInt(containerIndex));
                values.put(LauncherSettings.Favorites.ITEM_TYPE, c.getInt(itemTypeIndex));
                values.put(LauncherSettings.Favorites.APPWIDGET_ID, -1);
                values.put(LauncherSettings.Favorites.SCREEN, c.getInt(screenIndex));
                values.put(LauncherSettings.Favorites.CELLX, c.getInt(cellXIndex));
                values.put(LauncherSettings.Favorites.CELLY, c.getInt(cellYIndex));
                values.put(LauncherSettings.Favorites.URI, c.getString(uriIndex));
                values.put(LauncherSettings.Favorites.DISPLAY_MODE, c.getInt(displayModeIndex));
                rows[i++] = values;
            }

            db.beginTransaction();
            int total = 0;
            try {
                int numValues = rows.length;
                for (i = 0; i < numValues; i++) {
                    if (dbInsertAndCheck(this, db, mCurrentTable, null, rows[i]) < 0) {
                        return 0;
                    } else {
                        total++;
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            return total;
        }

        private void mappingOldPkgToNew(String intentStr, String conditionStr, SQLiteDatabase db){
            String sql = "UPDATE favorites SET intent='" +
                    intentStr +
                    "' WHERE intent like '" +
                    conditionStr +
                    "'";
            db.execSQL(sql);
        }
        
        /*YUNOS BEGIN*/
        //##date:2013/12/03 ##authorhongxing.whx ##BugId: 70021
        // support fota from 2.0, 2.1/2.3 to 2.5
        private HashMap<Integer, int[]> getDataMap(SQLiteDatabase db) {
            Log.d(TAG, "sxsexe----> ++++++++ getDataMap ");
            String sql = "select * from favorites where screen > 0";
            Cursor cursor = db.rawQuery(sql, null);
            HashMap<Integer, int[]> allData = new HashMap<Integer, int[]>();
            
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
                    Log.d(TAG, "sxsexe--->getDataMap id " + id + " screen " + indexes[0] + " cellX " + indexes[1] + " cellY " + indexes[2]);
                    allData.put(id, indexes);
                }
                cursor.close();
            }
            Log.d(TAG, "sxsexe----> -------- getDataMap ");
            return allData;
        }
        private void updateItemPosition(SQLiteDatabase db, int id, int[] position) {
            Log.d(TAG, "sxsexe----> ++++++++ updateItemPosition ");
            String updateSQL = "UPDATE favorites SET cellY=?,cellX=?,screen=? WHERE _id=?";
            db.execSQL(updateSQL, new String[]{String.valueOf(position[2]), String.valueOf(position[1]), String.valueOf(position[0]), String.valueOf(id)});
            Log.d(TAG, "sxsexe----> -------- updateItemPosition ");
        }

        /*YUNOS BEGIN*/
        //##module(HomeShell)
        //##date:2014/03/25 ##author:hao.liuhaolh@alibaba-inc.com##BugID:104271
        //fota from 2.7.1 to 3.0, some system apk's package name changed
        private void convertFrom2001To3000(SQLiteDatabase db) {
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
                    "component=com.autonavi.minimap.custom/com.autonavi.minimap.Splashy;end";
            conditionStr = "%com.autonavi.minimap%";
            mappingOldPkgToNew(intentStr, conditionStr, db);
        }
        /*YUNOS END*/

        private void convertFrom1001To2000(SQLiteDatabase db) {
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

        private void convertFrom1000To2000(SQLiteDatabase db) {
            Log.d(TAG, "doDataTransferFrom1000To1001");
            
            String sql;
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

            /*YUNOS BEGIN*/
            //##date:2014/01/23 ##author:hao.liuhaolh ##BugID:86995
            //browser and image icon position change after fota
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

            // 
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
            /* YUNOS BEGIN */
            //##date:2013/12/4 ##author:hongxing.whx ##bugid: 70388
            //theme center
            intentStr = "#Intent;action=android.intent.action.MAIN;" +
                    "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                    "component=com.yunos.theme.thememanager/.ThemeManagerSlideActivity;end";
            conditionStr = "%=com.aliyun.homeshell/com.yunos.theme.thememanager.ThemeManagerSlideActivity%";
            mappingOldPkgToNew(intentStr, conditionStr, db);
            /* YUNOS END */

            sql = "UPDATE favorites SET container = '-101' ,cellY='0' WHERE screen=0 and cellY=3";
            db.execSQL(sql);

            sql = "UPDATE favorites SET cellY='3' WHERE screen=0 and cellY=4";
            db.execSQL(sql);

            HashMap<Integer, int[]> dataMap = getDataMap(db);

            PositioReOrgnizenHelper.setAllPositions(dataMap);
            Set keys = dataMap.keySet();
            if(keys != null) {
                Iterator<Integer> iterator = keys.iterator();
//                int index = 0;
                while (iterator.hasNext()) {
//                    index++;
                    Integer id = (Integer) iterator.next();
                    int[] newPositions = PositioReOrgnizenHelper.getNewPosition(id);
                    /*YUNOS BEGIN*/
                    //##date:2014/01/24 ##author:hao.liuhaolh ##BugID:87053
                    //database update exception in fota
                    if (newPositions == null) {
                        Log.d(TAG, "newPositions is null, id is:" + id);
                        continue;
                    }
                    /*YUNOS END*/
                    Log.d(TAG, "id="+id+",newPositions[0]="+newPositions[0]+",newPositions[1]="+
                                   newPositions[1]+",newPositions[2]="+newPositions[2]);
                    if(newPositions[0] <= 9) {
                        updateItemPosition(db, id, newPositions);
                    } else {
                        sql = "delete from favorites where _id="+id;
                        Log.d(TAG, "sxsexe----> ++++++++ screen is full, delete from db ");
                        db.execSQL(sql);
                    }
                    /*YUNOS END*/
                }
                
                dataMap.clear();
            }
        }

        /*YUNOS BEGIN*/
        //##date:2014/5/22 ##author:zhangqiang.zq
        // smart search
        private void convertFrom3000To3010(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            String sql = "alter table favorites add fullPinyin char(100)";
            db.execSQL(sql);

            sql = "alter table favorites add shortPinyin char(30)";
            db.execSQL(sql);

            HanziToPinyin.getInstance().initHanziPinyinForAllChars(mContext);

            Cursor cursor = db.query("favorites", new String[]{
                    LauncherSettings.BaseLauncherColumns._ID,
                    LauncherSettings.BaseLauncherColumns.TITLE},
                    LauncherSettings.BaseLauncherColumns.ITEM_TYPE+"=0 or " +
                    LauncherSettings.BaseLauncherColumns.ITEM_TYPE+"=1 or " +
                    LauncherSettings.BaseLauncherColumns.ITEM_TYPE+"=9 or " +
                    LauncherSettings.BaseLauncherColumns.ITEM_TYPE+"=5", null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while(cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    long id = cursor.getLong(0);
                    updateTitlePinyin(values, cursor.getString(1));
                    db.update("favorites", values, LauncherSettings.BaseLauncherColumns._ID + "=" + id, null);
                }
                cursor.close();
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2013/12/06 ##authorhongxing.whx ##BugId: 71713
        // update bookmark itemType for fota from 2.0, 2.1/2.3 to 2.5
        private void updateBookmarkItems(SQLiteDatabase db) {
            try {
                /*YUNOS BEGIN*/
                //##date:2014/01/21 ##author:hao.liuhaolh ##BugID:87488
                //cloud app icon lost in fota
                String sql = "UPDATE favorites SET itemType = '1' WHERE itemType=6 or itemType=5";
                /*YUNOS END*/
                db.execSQL(sql);
            } catch (Exception e) {
                Log.e(TAG, "Failed in updateBookmarkItems : " + e.getMessage());
            }
        }
        /*YUNOS END*/
        
        //added by dongjun for fota downloading item begin
        private void updateDownloadingItems(SQLiteDatabase db) {
            try {
                String updateSQL = "UPDATE favorites SET intent=? WHERE _id=?";
                String sql = "select * from favorites where itemType = '8' ";
                Log.d(TAG, "updateDownloadingItems");
                Cursor cursor = db.rawQuery(sql, null);
                if(cursor != null) {
                    while(cursor.moveToNext()) {
                        int idIndex = cursor.getColumnIndexOrThrow(Favorites._ID);
                        int intentindex = cursor.getColumnIndexOrThrow(Favorites.INTENT);
                        int id = cursor.getInt(idIndex);
                        String intent = cursor.getString(intentindex);
                        intent = intent.replace("packageName=", "packagename=");
                        db.execSQL(updateSQL, new String[]{String.valueOf(intent), String.valueOf(id)});
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in updateDownloadingItems : " + e.getMessage());
            }
        }
        //added by dongjun for fota downloading item end
        
        private void updateDockItems(SQLiteDatabase db) {
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
                        Log.d(TAG, "sxsexe--->getDataMap id " + id + " screen " + indexes[0] + " cellX " + indexes[1] + " cellY " + indexes[2]);
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed in updateDockItems : " + e.getMessage());
            }
        }

        /*YUNOS BEGIN LH*/
        private void updateSdcardAppItems(SQLiteDatabase db) {
            String updateSQL = "UPDATE favorites SET isShortcut=? WHERE _id=?";
            String sql = "select * from favorites where itemType = '0' ";
            Log.d(TAG, "updateSdcardAppItems");
            Cursor cursor = db.rawQuery(sql, null);
            if(cursor != null) {
                while(cursor.moveToNext()) {
                    int idIndex = cursor.getColumnIndexOrThrow(Favorites._ID);
                    int iconIndex = cursor.getColumnIndexOrThrow(Favorites.ICON);
                    byte[] data = cursor.getBlob(iconIndex);
                    if (data == null || data.length == 0) {
                        continue;
                    }
                    int id = cursor.getInt(idIndex);
                    db.execSQL(updateSQL, new String[]{String.valueOf(1), String.valueOf(id)});
                }
                cursor.close();
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/03/15 ##author:hao.liuhaolh ##BugID:101011
        //handle gadget type in restore and fota upgrade
        private void updateGadgetItems(SQLiteDatabase db) {
            Log.d(TAG, "updateGadgetItems");
            try {
                String sql = "UPDATE favorites SET itemType = '10', title = 'clock_4x1' WHERE itemType=7";
                db.execSQL(sql);
            } catch (Exception e) {
                Log.e(TAG, "Failed in updateBookmarkItems : " + e.getMessage());
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/03/18 ##author:hao.liuhaolh ##BugID:101768
        //launcher provider downgrade exception
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onDowngrade is called, oldVersion: " + oldVersion + " newVersion: " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            db.execSQL(AppLaunchDBHelper.SQL_DROP_APP_LAUNCH_TABLE);
            db.execSQL(LastLaunchTimeHelper.SQL_DROP_LAST_LAUNCH_TABLE);
            onCreate(db);
        }
        /*YUNOS END*/

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { 
            if (LOGD) Log.d(TAG, "onUpgrade triggered, oldeversion:" + oldVersion);
            if (LOGD) Log.d(TAG, "onUpgrade triggered, newVersion:" + newVersion);
            int version = oldVersion;
            if (newVersion < version) {
                Log.e(TAG, "oldversion>newVersion, oldeversion:" + oldVersion+",newVersion:" +newVersion);
                return;
            }

            /*YUNOS BEGIN*/
            //##date:2014/03/15 ##author:hao.liuhaolh ##BugID:101011
            //handle gadget type in restore and fota upgrade
            if (newVersion < 3000) {
                Log.e(TAG, "newVersion < 3000");
                return;
            }
            /*YUNOS END*/
            try {
                if (version < 1000) {
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
                    onCreate(db);
                    return;
                }

                // newVersion must be >= 2000
                if (version < 2000) {
                    // 1000: codebase 2.0
                    if (version == 1000) {
                        convertFrom1000To2000(db);
                    }
                    //1001: codebase 2.1 and 2.3
                    if (version == 1001) {
                        convertFrom1001To2000(db);
                    }
                    updateDownloadingItems(db);
                    updateDockItems(db);
                    //##date:2013/12/06 ##author:hongxing.whx ##BugId: 71713
                    // update bookmark itemType for fota from 2.0, 2.1/2.3 to 2.5
                    updateBookmarkItems(db);
                    /*YUNOS END*/
                    /*YUNOS BEGIN*/
                    //##date:2013/12/13 ##author:hao.liuhaolh ##BugID:73662
                    // fato sd app icon error
                    updateSdcardAppItems(db);
                    /*YUNOS END*/
                }
                // 2000: codebase 2.5
                if (version < 2001) {
                    String intentStr, conditionStr;
                    Log.d(TAG,"Update from 2.5 to 2.7");
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

                /*YUNOS BEGIN*/
                //##date:2014/03/15 ##author:hao.liuhaolh ##BugID:101011
                //handle gadget type in restore and fota upgrade
                if (version < 3000) {
                    Log.d(TAG,"Update from 2.7 to 3.0");
                    updateGadgetItems(db);
                    /*YUNOS BEGIN*/
                    //##module(HomeShell)
                    //##date:2014/03/25 ##author:hao.liuhaolh@alibaba-inc.com##BugID:104271
                    //fota from 2.7.1 to 3.0, some system apk's package name changed
                    convertFrom2001To3000(db);
                    /*YUNOS END*/
                }
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //##date:2014/5/22 ##author:zhangqiang.zq
                // smart search
                if (version < 3010) {
                    Log.d(TAG,"Update from 3000 to 3010");
                    convertFrom3000To3010(db);
                }
                /*YUNOS END*/
                if (version < 3015) {
                    Log.d(TAG,"Update to 3010");
                    String intentStr, conditionStr;
                    intentStr = "#Intent;action=android.intent.action.MAIN;" +
                            "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                            "component=com.UCMobile/.main.UCMobile;end";
                    conditionStr = "%com.UCMobile.yunos/com.UCMobile.main.UCMobile%";
                    mappingOldPkgToNew(intentStr, conditionStr, db);

                }
				
                /*YUNOS BEGIN*/
                //##date:2014/5/22 ##author:zhangqiang.zq
                // favorite app
                if (version < 3030) {
                    Log.d(TAG,"Update from 3015 to 3030");
                    convertFrom3010To3030(db);
                }
                /*YUNOS END*/

               if (version < 3035) {
                    // gaode map changed from custom to public version, package name changed
                    Log.d(TAG,"Update to 3035");
                    String intentStr, conditionStr;
                    intentStr = "#Intent;action=android.intent.action.MAIN;" +
                            "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                            "component=com.autonavi.minimap/.Splashy;end";
                    conditionStr = "%component=com.autonavi.minimap.custom/com.autonavi.minimap.Splashy;end%";
                    mappingOldPkgToNew(intentStr, conditionStr, db);
               }

               if (version < 3040) {
                    // huanji package name changed
                    Log.d(TAG,"Update to 3040, huanji package changed to yunos special version");
                    String intentStr, conditionStr;
                    intentStr = "#Intent;action=android.intent.action.MAIN;" +
                            "category=android.intent.category.LAUNCHER;launchFlags=0x10200000;" +
                            "component=com.stkj.dianchuan/.home.ActivityHome;end";
                    conditionStr = "%component=cn.huanji/.MainActivity;end%";
                    mappingOldPkgToNew(intentStr, conditionStr, db);
               }
                /*YUNOS BEGIN*/
                //##date:2014/5/22 ##author:zhangqiang.zq
                // smart search
                if (version < 3050) {
                    Log.d(TAG,"Update from 3000 to 3010");
                    convertFrom3040To3050(db);
                }
                /*YUNOS END*/

                /*YUNOS BEGIN*/
                //BugID:5782788 ##date:2015/2/10 ##author:hao.liuhaolh
                //for search widget
                if (version < 3055) {
                    Log.d(TAG,"Update from 3050 to 3055");
                    convertFrom3050To3055(db);
                }
                /*YUNOS END*/
                //for pad land mode
                if (version < 3060)
                {
                    Log.d(TAG,"Update from 3050 to 3060");
                    convertFrom3055To3060(db);
                }

                /* YUNOS BEGIN */
                // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6348948
                // Smart app recommendation (to record app launch times and
                // counts)
                if (version < 3061) {
                    Log.d(TAG, "Update from 3060 to 3061");
                    convertFrom3060To3061(db);
                }

                if (version < 3062) {
                    Log.d(TAG, "Update from 3061 to 3062");
                    convertFrom3061To3062(db);
                }
                /* YUNOS END */

                /* YUNOS BEGIN */
                if (version < 3063) {
                    Log.d(TAG, "Update from 3062 to 3063");
                    convertFrom3062To3063(db);
                }
                /* YUNOS END */
                if (version < 3064) {
                    Log.d(TAG, "Update from 3062 to 3063");
                    convertFrom3063To3064(db);
                }
                /* YUNOS BEGIN */
                // ## date: 2016/09/06 ## author: yongxing.lyx
                // ## BugID:8826794:crash after upgrade.
                checkAllColums(db);
                /* YUNOS END */
            } catch (Exception e) {
                Log.e(TAG, "Destroying all old data. ", e);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
                onCreate(db);
            }
        }
        /*YUNOS END*/

        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // favorite app
        private void convertFrom3010To3030(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            Log.d(TAG,"Update from 3015 to 3030: create favoriteWeight and favoriteIcon");
            db.execSQL("alter table favorites add favoriteWeight INTEGER DEFAULT 0");
            db.execSQL("alter table favorites add favoriteIcon BLOB");
        }
        /*YUNOS END*/
        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        private void convertFrom3040To3050(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE aged_favorites AS SELECT * FROM favorites WHERE 1=2");
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED, true);
            editor.commit();
        }

        //for pad land mode
        private void convertFrom3055To3060(SQLiteDatabase db) {
            String sql = "alter table favorites add cellXLand INTEGER";
            db.execSQL(sql);
            sql = "alter table favorites add cellYLand INTEGER";
            db.execSQL(sql);
            sql = "alter table aged_favorites add cellXLand INTEGER";
            db.execSQL(sql);
            sql = "alter table aged_favorites add cellYLand INTEGER";
            db.execSQL(sql);
        }

        /* YUNOS BEGIN */
        // ##date:2015/8/21 ##author:zhanggong.zg ##BugID:6348948
        // Smart app recommendation (to record app launch times and counts)
        private void convertFrom3060To3061(SQLiteDatabase db) {
            Log.d(TAG, "convertFrom3060To3061 in");
            db.execSQL(AppLaunchDBHelper.SQL_DROP_APP_LAUNCH_TABLE);
            db.execSQL(AppLaunchDBHelper.SQL_CREATE_APP_LAUNCH_TABLE);
            AppLaunchUpdater.setupAlarm(mContext);
            Log.d(TAG, "convertFrom3060To3061 out");
        }

        private void convertFrom3061To3062(SQLiteDatabase db) {
            Log.d(TAG, "convertFrom3061To3062 in");
            db.execSQL(LastLaunchTimeHelper.SQL_DROP_LAST_LAUNCH_TABLE);
            db.execSQL(LastLaunchTimeHelper.SQL_CREATE_LAST_LAUNCH_TABLE);

            /* YUNOS BEGIN */
            // ##date:2015/9/1 ##author:zhanggong.zg ##BugID:6384589
            // used to support smart app suggestion (since version 3061)
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(DATABASE_CREATION_TIME, Calendar.getInstance().getTimeInMillis());
            editor.commit();
            /* YUNOS END */
            Log.d(TAG, "convertFrom3061To3062 out");
        }
        /* YUNOS END */

        private void convertFrom3062To3063(SQLiteDatabase db) {
            Log.d(TAG, "convertFrom3062To3063 in");
            String sql = "select count(*) from " + TABLE_FAVORITES + " where container=?";
            String strHotseatContainer = String.valueOf(LauncherSettings.Favorites.CONTAINER_HOTSEAT);
            Cursor cursor = db.rawQuery(sql, new String[]{strHotseatContainer});
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                if (count == 5) {
                    sql = "select _id from " + TABLE_FAVORITES + " where container=" + strHotseatContainer + " order by screen desc ";
                    cursor = db.rawQuery(sql, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int id = cursor.getInt(0);
                        if (id > 0) {
                            db.execSQL("delete from favorites where _id=" + id);
                        }
                        cursor.close();
                    }
                    ConfigManager.setHotseatMaxCountX(mContext, ConfigManager.DEFAULT_HOTSEAT_MAX_COUNT_X);
                }
            }
            Log.d(TAG, "convertFrom3062To3063 out");
        }

        private void convertFrom3063To3064(SQLiteDatabase db) {
            Log.d(TAG,"Update from 3063 to 3034: create userId");
            db.execSQL("alter table favorites add userId INTEGER");
            db.execSQL("alter table aged_favorites add userId INTEGER");
        }

        /* YUNOS BEGIN */
        // ## date: 2016/09/06 ## author: yongxing.lyx
        // ## BugID:8826794:crash after upgrade.
        private void checkAllColums(SQLiteDatabase db) {
            Log.d(TAG, "checkAllColums() start");
            String table = TABLE_FAVORITES;
            Cursor c = null;
            try {
                c = db.query(table, null, null, null, null, null, null, "1");
                if (c != null && c.moveToFirst()) {
                    int columIndex;
                    columIndex = c.getColumnIndex(Favorites.TITLE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add TITLE");
                        db.execSQL("alter table favorites add title TEXT");
                        db.execSQL("alter table aged_favorites add title TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.INTENT);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add INTENT");
                        db.execSQL("alter table favorites add intent TEXT");
                        db.execSQL("alter table aged_favorites add intent TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.CONTAINER);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CONTAINER");
                        db.execSQL("alter table favorites add container INTEGER");
                        db.execSQL("alter table aged_favorites add container INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.SCREEN);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add SCREEN");
                        db.execSQL("alter table favorites add screen INTEGER");
                        db.execSQL("alter table aged_favorites add screen INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.CELLX);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CELLX");
                        db.execSQL("alter table favorites add cellX INTEGER");
                        db.execSQL("alter table aged_favorites add cellX INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.CELLY);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CELLY");
                        db.execSQL("alter table favorites add cellY INTEGER");
                        db.execSQL("alter table aged_favorites add cellY INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.SPANX);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add SPANX");
                        db.execSQL("alter table favorites add spanX INTEGER");
                        db.execSQL("alter table aged_favorites add spanX INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.SPANY);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add SPANY");
                        db.execSQL("alter table favorites add spanY INTEGER");
                        db.execSQL("alter table aged_favorites add spanY INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.ITEM_TYPE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add ITEM_TYPE");
                        db.execSQL("alter table favorites add itemType INTEGER");
                        db.execSQL("alter table aged_favorites add itemType INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.APPWIDGET_ID);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add APPWIDGET_ID");
                        db.execSQL("alter table favorites add appWidgetId INTEGER NOT NULL DEFAULT -1");
                        db.execSQL("alter table aged_favorites add appWidgetId INTEGER NOT NULL DEFAULT -1");
                    }
                    // columIndex = c.getColumnIndex(Favorites.IS_SHORTCUT);
                    // if (columIndex == -1) {
                    // Log.i(TAG, "checkAllColums() add IS_SHORTCUT");
                    // db.execSQL("alter table favorites add isShortcut INTEGER DEFAULT 0");
                    // db.execSQL("alter table aged_favorites add isShortcut INTEGER DEFAULT 0");
                    // }
                    columIndex = c.getColumnIndex(Favorites.ICON_TYPE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add ICON_TYPE");
                        db.execSQL("alter table favorites add iconType INTEGER");
                        db.execSQL("alter table aged_favorites add iconType INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.ICON_PACKAGE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add ICON_PACKAGE");
                        db.execSQL("alter table favorites add iconPackage TEXT");
                        db.execSQL("alter table aged_favorites add iconPackage TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.ICON_RESOURCE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add ICON_RESOURCE");
                        db.execSQL("alter table favorites add iconResource TEXT");
                        db.execSQL("alter table aged_favorites add iconResource TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.ICON);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add ICON");
                        db.execSQL("alter table favorites add icon BLOB");
                        db.execSQL("alter table aged_favorites add icon BLOB");
                    }
                    columIndex = c.getColumnIndex(Favorites.URI);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add URI");
                        db.execSQL("alter table favorites add uri TEXT");
                        db.execSQL("alter table aged_favorites add uri TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.DISPLAY_MODE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add DISPLAY_MODE");
                        db.execSQL("alter table favorites add displayMode INTEGER");
                        db.execSQL("alter table aged_favorites add displayMode INTEGER");
                    }
                    // add by yunos
                    columIndex = c.getColumnIndex(Favorites.CELLXLAND);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CELLXLAND");
                        db.execSQL("alter table favorites add cellXLand INTEGER");
                        db.execSQL("alter table aged_favorites add cellXLand INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.CELLYLAND);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CELLYLAND");
                        db.execSQL("alter table favorites add cellYLand INTEGER");
                        db.execSQL("alter table aged_favorites add cellYLand INTEGER");
                    }
                    columIndex = c.getColumnIndex(Favorites.CAN_DELEDE);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add CAN_DELEDE");
                        db.execSQL("alter table favorites add canDelete INTEGER DEFAULT 1");
                        db.execSQL("alter table aged_favorites add canDelete INTEGER DEFAULT 1");
                    }
                    columIndex = c.getColumnIndex(Favorites.MESSAGE_NUM);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add MESSAGE_NUM");
                        db.execSQL("alter table favorites add messageNum INTEGER DEFAULT 0");
                        db.execSQL("alter table aged_favorites add messageNum INTEGER DEFAULT 0");
                    }
                    columIndex = c.getColumnIndex(Favorites.IS_NEW);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add IS_NEW");
                        db.execSQL("alter table favorites add isNew INTEGER DEFAULT 0");
                        db.execSQL("alter table aged_favorites add isNew INTEGER DEFAULT 0");
                    }
                    columIndex = c.getColumnIndex(Favorites.FULL_PINYIN);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add FULL_PINYIN");
                        db.execSQL("alter table favorites add fullPinyin TEXT");
                        db.execSQL("alter table aged_favorites add fullPinyin TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.SHORT_PINYIN);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add SHORT_PINYIN");
                        db.execSQL("alter table favorites add shortPinyin TEXT");
                        db.execSQL("alter table aged_favorites add shortPinyin TEXT");
                    }
                    columIndex = c.getColumnIndex(Favorites.FAVORITE_WEIGHT);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add FAVORITE_WEIGHT");
                        db.execSQL("alter table favorites add favoriteWeight INTEGER DEFAULT 0");
                        db.execSQL("alter table aged_favorites add favoriteWeight INTEGER DEFAULT 0");
                    }
                    columIndex = c.getColumnIndex(Favorites.FAVORITE_ICON);
                    if (columIndex == -1) {
                        Log.i(TAG, "checkAllColums() add FAVORITE_ICON");
                        db.execSQL("alter table favorites add favoriteIcon BLOB");
                        db.execSQL("alter table aged_favorites add favoriteIcon BLOB");
                    }
                }
            } catch (android.database.sqlite.SQLiteException sqlE) {
                sqlE.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                    c = null;
                }
            }
            Log.d(TAG, "checkAllColums() end");
        }
        /* YUNOS END */

        public void setAgedMode(boolean aged) {
            mCurrentTable = aged ? TABLE_AGED_FAVORITES : TABLE_FAVORITES;
        }

        public void syncMessageNumbers() {
            SQLiteDatabase db = getWritableDatabase();
            String srcTable = mDbAgedMode ? TABLE_FAVORITES : TABLE_AGED_FAVORITES;
            Cursor c = db.query(srcTable, new String[] {Favorites.INTENT, Favorites.MESSAGE_NUM},
                    Favorites.MESSAGE_NUM + ">=0", null, null, null, null);
            if (c != null && c.getCount() > 0) {
                db.beginTransaction();
                while (c.moveToNext()) {
                    String intent = c.getString(0);
                    int msgNumber = c.getInt(1);
                    ContentValues values = new ContentValues();
                    values.put(Favorites.MESSAGE_NUM, msgNumber);
                    db.update(mCurrentTable, values, Favorites.INTENT + "='" + intent + "'", null);
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            }

            if (c != null) {
                c.close();
            }
        }

        private String mCurrentTable = mDbAgedMode ? TABLE_AGED_FAVORITES : TABLE_FAVORITES;;

        /*YUNOS END*/

        private void convertFrom3050To3055(SQLiteDatabase db) {
            Log.d(TAG, "convertFrom3050To3055 in");
            Cursor c = db.query("favorites", null, "screen=? and container=?",
                    new String[] { String.valueOf(0), String.valueOf(-100)}, null, null, null);
            final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
            final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
            int y;
            int spany;
            boolean isOccupied = false;
            while (c.moveToNext() != false) {
                y = c.getInt(cellYIndex);
                spany = c.getInt(spanYIndex);
                if ((y == 1) || (y ==0 && spany > 1)) {
                    Log.d(TAG, "y is " + y + " spany is " + spany);
                    isOccupied = true;
                    break;
                }
            }

            if (isOccupied == false) {
                if (mMaxId == -1) {
                    mMaxId = initializeMaxId(db);
                }
                String packageName = "com.yunos.alimobilesearch";
                String className = "com.yunos.alimobilesearch.widget.SearchWidgetProvider";
                PackageManager packageManager = mContext.getPackageManager();

                boolean hasPackage = true;
                ComponentName cn = new ComponentName(packageName, className);
                try {
                    packageManager.getReceiverInfo(cn, 0);
                } catch (Exception e) {
                    String[] packages = packageManager.currentToCanonicalPackageNames(
                            new String[] { packageName });
                    cn = new ComponentName(packages[0], className);
                    try {
                        packageManager.getReceiverInfo(cn, 0);
                    } catch (Exception e1) {
                        hasPackage = false;
                    }
                }

                if (hasPackage == true) {
                    //add search widget in db
                    Log.d(TAG, "add search widget in db");
                    ContentValues values = new ContentValues();     
                    values.put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP);
                    values.put(Favorites.SCREEN, 0);
                    values.put(Favorites.CELLX, 0);
                    values.put(Favorites.CELLY, 1);
                    values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPWIDGET);
                    addAppWidget(db, values, cn, 4, 1, null);
                }
            }
        }

        /*YUNOS BEGIN*/
        //##date:2014/7/11 ##author:andy.zx
        //##date:2014/11/25 ##author:zhanggong.zg ##BugID:5597226
        // aged mode
        /**
         * Synchronous aged and normal database when changed
         */
        public void syncHideseat() {
            SQLiteDatabase db = getWritableDatabase();
            String srcTable = mDbAgedMode ? TABLE_FAVORITES : TABLE_AGED_FAVORITES;
            Map<String, ContentValues> oldHideseatItems = new HashMap<String, ContentValues>();
            db.beginTransaction();

            /* YUNOS BEGIN */
            // ##date:2014/11/25 ##author:zhanggong.zg ##BugID:5597226
            // temporarily store the information (specially, message number) of current hideseat items
            Cursor c = db.query(mCurrentTable, null, "container=? and messageNum>0",
                    new String[] { String.valueOf(Favorites.CONTAINER_HIDESEAT) }, null, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String intent = c.getString(c.getColumnIndex(Favorites.INTENT));
                    oldHideseatItems.put(intent, createContentValuesFromCursor(c));
                }
                c.close();
            }
            Log.v(TAG, "syncHideseat: total items with msgNum = " + oldHideseatItems.size());
            /* YUNOS END */

            // delete all hideseat items in current table
            db.delete(mCurrentTable, Favorites.CONTAINER + "=" + Favorites.CONTAINER_HIDESEAT, null);

            // synchronize hideseat items from source table to current table
            c = db.query(srcTable, null,
                    Favorites.CONTAINER + "=" + Favorites.CONTAINER_HIDESEAT, null, null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    String intent = c.getString(c.getColumnIndex(Favorites.INTENT));
                    ContentValues values = createContentValuesFromCursor(c);
                    values.put(LauncherSettings.Favorites._ID, generateNewId());
                    if (isInHidesead(mCurrentTable, intent)) {
                        db.delete(mCurrentTable, Favorites.INTENT + "='" + intent + "'", null);
                    }
                    db.insert(mCurrentTable, null, values);
                    oldHideseatItems.remove(intent);
                }
            }

            if (c != null) {
                c.close();
            }

            /* YUNOS BEGIN */
            // ##date:2014/11/25 ##author:zhanggong.zg ##BugID:5597226
            // insert items which are dragged out of hideseat
            if (!oldHideseatItems.isEmpty()) {
                Iterator<ContentValues> itr = oldHideseatItems.values().iterator();
                while (itr.hasNext()) {
                    ContentValues values = itr.next();
                    values.put(LauncherSettings.Favorites._ID, generateNewId());
                    // Note that we set the new item as no-space-application, so it will be
                    // automatically moved to appropriate position by LauncherModel later.
                    values.put(LauncherSettings.Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_NOSPACE_APPLICATION);
                    values.put(LauncherSettings.Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP);
                    values.put(LauncherSettings.Favorites.SCREEN, -1);
                    values.put(LauncherSettings.Favorites.CELLX, -1);
                    values.put(LauncherSettings.Favorites.CELLY, -1);
                    db.insert(mCurrentTable, null, values);
                }
            }
            Log.v(TAG, "syncHideseat: items that move to workspace with msgNum = " + oldHideseatItems.size());
            /* YUNOS END */

            db.setTransactionSuccessful();
            db.endTransaction();
        }

        private ContentValues createContentValuesFromCursor(Cursor c) {
            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites.INTENT, c.getString(c.getColumnIndex(Favorites.INTENT)));
            values.put(LauncherSettings.Favorites.TITLE, c.getString(c.getColumnIndex(Favorites.TITLE)));
            values.put(LauncherSettings.Favorites.ICON_TYPE, c.getInt(c.getColumnIndex(Favorites.ICON_TYPE)));
            values.put(LauncherSettings.Favorites.ICON, c.getBlob(c.getColumnIndex(Favorites.ICON)));
            values.put(LauncherSettings.Favorites.ICON_PACKAGE, c.getString(c.getColumnIndex(Favorites.ICON_PACKAGE)));
            values.put(LauncherSettings.Favorites.ICON_RESOURCE, c.getString(c.getColumnIndex(Favorites.ICON_RESOURCE)));
            values.put(LauncherSettings.Favorites.CONTAINER, Favorites.CONTAINER_HIDESEAT);
            values.put(LauncherSettings.Favorites.ITEM_TYPE, c.getInt(c.getColumnIndex(Favorites.ITEM_TYPE)));
            values.put(LauncherSettings.Favorites.APPWIDGET_ID, c.getInt(c.getColumnIndex(Favorites.APPWIDGET_ID)));
            values.put(LauncherSettings.Favorites.SCREEN, c.getInt(c.getColumnIndex(Favorites.SCREEN)));
            values.put(LauncherSettings.Favorites.CELLX, c.getInt(c.getColumnIndex(Favorites.CELLX)));
            values.put(LauncherSettings.Favorites.CELLY, c.getInt(c.getColumnIndex(Favorites.CELLY)));
            values.put(LauncherSettings.Favorites.URI, c.getString(c.getColumnIndex(Favorites.URI)));
            values.put(LauncherSettings.Favorites.DISPLAY_MODE, c.getInt(c.getColumnIndex(Favorites.DISPLAY_MODE)));
            values.put(LauncherSettings.Favorites.MESSAGE_NUM, c.getInt(c.getColumnIndex(Favorites.MESSAGE_NUM)));
            return values;
        }

        /**
         * Check is the item data of the table in hidesead
         * @param table
         * @param intentStr
         * @return
         */
        private boolean isInHidesead(String table, String intentStr) {
            boolean isInHidesead = false;
            SQLiteDatabase db = getWritableDatabase();
            Cursor c = db.query(table, new String[] {Favorites.CONTAINER},
                    Favorites.INTENT + "='" + intentStr +"'", null, null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    isInHidesead = true;
                }
            }
            if (c != null) {
                c.close();
            }
            return isInHidesead;
        }
        /*YUNOS END*/

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewId() {
            if (mMaxId < 0) {
                throw new RuntimeException("Error: max id was not initialized");
            }
            mMaxId += 1;
            return mMaxId;
        }

        private long initializeMaxId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM favorites", null);

            // get the result
            final int maxIdIndex = 0;
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(maxIdIndex);
            }
            if (c != null) {
                c.close();
            }

            /*YUNOS BEGIN*/
            //##date:2014/7/8 ##author:zhangqiang.zq
            // aged mode
            c = db.rawQuery("SELECT MAX(_id) FROM aged_favorites", null);
            long idAged = -1;
            if (c != null && c.moveToNext()) {
                idAged = c.getLong(maxIdIndex);
            }
            if (c != null) {
                c.close();
            }
            id = id > idAged ? id : idAged;
            /*YUNOS END*/

            if (id == -1) {
                throw new RuntimeException("Error: could not query max id");
            }

            return id;
        }

        /**
         * Upgrade existing clock and photo frame widgets into their new widget
         * equivalents.
         */
        private void convertWidgets(SQLiteDatabase db) {
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            final int[] bindSources = new int[] {
                    Favorites.ITEM_TYPE_WIDGET_CLOCK,
                    Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME,
                    Favorites.ITEM_TYPE_WIDGET_SEARCH,
            };

            final String selectWhere = buildOrWhereString(Favorites.ITEM_TYPE, bindSources);

            Cursor c = null;

            db.beginTransaction();
            try {
                // Select and iterate through each matching widget
                c = db.query(mCurrentTable, new String[] { Favorites._ID, Favorites.ITEM_TYPE },
                        selectWhere, null, null, null, null);
                /* YUNOS BEGIN */
                // ##date:2014/4/23 ##author:hongchao.ghc ##BugID:111144
                if (LOGD && c != null)
                    Log.d(TAG, "found upgrade cursor count=" + c.getCount());
                /* YUNOS END */
                final ContentValues values = new ContentValues();
                while (c != null && c.moveToNext()) {
                    long favoriteId = c.getLong(0);
                    int favoriteType = c.getInt(1);

                    // Allocate and update database with new appWidgetId
                    try {
                        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                        if (LOGD) {
                            Log.d(TAG, "allocated appWidgetId=" + appWidgetId
                                    + " for favoriteId=" + favoriteId);
                        }
                        values.clear();
                        values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPWIDGET);
                        values.put(Favorites.APPWIDGET_ID, appWidgetId);

                        // Original widgets might not have valid spans when upgrading
                        if (favoriteType == Favorites.ITEM_TYPE_WIDGET_SEARCH) {
                            values.put(LauncherSettings.Favorites.SPANX, 4);
                            values.put(LauncherSettings.Favorites.SPANY, 1);
                        } else {
                            values.put(LauncherSettings.Favorites.SPANX, 2);
                            values.put(LauncherSettings.Favorites.SPANY, 2);
                        }

                        String updateWhere = Favorites._ID + "=" + favoriteId;
                        db.update(mCurrentTable, values, updateWhere, null);

                        if (favoriteType == Favorites.ITEM_TYPE_WIDGET_CLOCK) {
                            // TODO: check return value
                            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                                    new ComponentName("com.android.alarmclock",
                                    "com.android.alarmclock.AnalogAppWidgetProvider"));
                        } else if (favoriteType == Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME) {
                            // TODO: check return value
                            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                                    new ComponentName("com.android.camera",
                                    "com.android.camera.PhotoAppWidgetProvider"));
                        } else if (favoriteType == Favorites.ITEM_TYPE_WIDGET_SEARCH) {
                            // TODO: check return value
                            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId,
                                    getSearchWidgetProvider());
                        }
                    } catch (RuntimeException ex) {
                        Log.e(TAG, "Problem allocating appWidgetId", ex);
                    }
                }

                db.setTransactionSuccessful();
            } catch (SQLException ex) {
                Log.w(TAG, "Problem while allocating appWidgetIds for existing widgets", ex);
            } finally {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
            }
        }

        private static final void beginDocument(XmlPullParser parser, String firstElementName)
                throws XmlPullParserException, IOException {
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                ;
            }

            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }

            if (!parser.getName().equals(firstElementName)) {
                throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() +
                        ", expected " + firstElementName);
            }
        }

        /**
         * Loads the default set of favorite packages from an xml file.
         *
         * @param db The database to write the values into
         * @param filterContainerId The specific container id of items to load
         */
        private int loadFavorites(SQLiteDatabase db, int workspaceResourceId) {
        	Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ContentValues values = new ContentValues();

            PackageManager packageManager = mContext.getPackageManager();
//            int allAppsButtonRank =
//                    mContext.getResources().getInteger(R.integer.hotseat_all_apps_index);
            int i = 0;
            try {
                XmlResourceParser parser = mContext.getResources().getXml(workspaceResourceId);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                beginDocument(parser, TAG_FAVORITES);

                final int depth = parser.getDepth();

                int type;
                while (((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

                    if (type != XmlPullParser.START_TAG) {
                        continue;
                    }

                    boolean added = false;
                    final String name = parser.getName();

                    TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);

                    long container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    if (a.hasValue(R.styleable.Favorite_container)) {
                        container = Long.valueOf(a.getString(R.styleable.Favorite_container));
                    }

                    String screen = a.getString(R.styleable.Favorite_screen);
                    String x = a.getString(R.styleable.Favorite_x);
                    String y = a.getString(R.styleable.Favorite_y);
                    String xLand = a.getString(R.styleable.Favorite_xLand);
                    String yLand = a.getString(R.styleable.Favorite_yLand);

                    // If we are adding to the hotseat, the screen is used as the position in the
                    // hotseat. This screen can't be at position 0 because AllApps is in the
                    // zeroth position.
                    /*commented by xiaodong.lxd
                     * if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                            && Integer.valueOf(screen) == allAppsButtonRank) {
                        throw new RuntimeException("Invalid screen position for hotseat item");
                    }*/

                    values.clear();
                    values.put(LauncherSettings.Favorites.CONTAINER, container);
                    values.put(LauncherSettings.Favorites.SCREEN, screen);
                    values.put(LauncherSettings.Favorites.CELLX, x);
                    values.put(LauncherSettings.Favorites.CELLY, y);
                    if (ConfigManager.isLandOrienSupport()) {
                        values.put(LauncherSettings.Favorites.CELLXLAND, xLand);
                        values.put(LauncherSettings.Favorites.CELLYLAND, yLand);
                    }
                    if (TAG_FAVORITE.equals(name)) {
                        long id = addAppShortcut(db, values, a, packageManager, intent);
                        added = id >= 0;
                    } else if (TAG_SEARCH.equals(name)) {
                        added = addSearchWidget(db, values);
                    } else if (TAG_CLOCK.equals(name)) {
                        added = addClockWidget(db, values);
                    } else if (TAG_APPWIDGET.equals(name)) {
                        added = addAppWidget(parser, attrs, type, db, values, a, packageManager);
                    } else if (TAG_SHORTCUT.equals(name)) {
                        long id = addUriShortcut(db, values, a);
                        added = id >= 0;
                    } else if (TAG_FOLDER.equals(name)) {
                        String title;
                        int titleResId =  a.getResourceId(R.styleable.Favorite_title, -1);
                        if (titleResId != -1) {
                            title = mContext.getResources().getString(titleResId);
                        } else {
                            /* YUNOS BEGIN */
                            // ## date: 2016/06/20 ## author: yongxing.lyx
                            // ## BugID:8431132:default folder auto change title
                            // when language changed.
                            title = a.getString(R.styleable.Favorite_title);
                            if (title == null || (title.length() > 0 && title.charAt(0) != '#')) {
                                title = mContext.getResources().getString(R.string.folder_name);
                            }
                            /* YUNOS END */
                        }
                        values.put(LauncherSettings.Favorites.TITLE, title);
                        long folderId = addFolder(db, values);
                        added = folderId >= 0;
                        if (mDbAgedMode) {
                            agedDefaultFolderId = folderId;
                        }
                        ArrayList<Long> folderItems = new ArrayList<Long>();

                        int folderDepth = parser.getDepth();
                        while ((type = parser.next()) != XmlPullParser.END_TAG ||
                                parser.getDepth() > folderDepth) {
                            if (type != XmlPullParser.START_TAG) {
                                continue;
                            }
                            final String folder_item_name = parser.getName();

                            TypedArray ar = mContext.obtainStyledAttributes(attrs,
                                    R.styleable.Favorite);
                            values.clear();
                            values.put(LauncherSettings.Favorites.CONTAINER, folderId);
                            String f_screen = ar.getString(R.styleable.Favorite_screen);
                            String f_x = ar.getString(R.styleable.Favorite_x);
                            String f_y = ar.getString(R.styleable.Favorite_y);
                            String f_xLand = ar.getString(R.styleable.Favorite_xLand);
                            String f_yLand = ar.getString(R.styleable.Favorite_yLand);
                            values.put(LauncherSettings.Favorites.SCREEN, f_screen);
                            values.put(LauncherSettings.Favorites.CELLX, f_x);
                            values.put(LauncherSettings.Favorites.CELLY, f_y);
                            if (ConfigManager.isLandOrienSupport()) {
                                values.put(LauncherSettings.Favorites.CELLXLAND, f_xLand);
                                values.put(LauncherSettings.Favorites.CELLYLAND, f_yLand);
                            }
                            int tmpScreen = Integer.valueOf(f_screen);
                            int tmpX = Integer.valueOf(f_x);
                            int tmpY = Integer.valueOf(f_y);
                            if (tmpScreen > sMaxPosAfterLoadFav.s) {
                                sMaxPosAfterLoadFav.s = tmpScreen;
                                sMaxPosAfterLoadFav.x = tmpX;
                                sMaxPosAfterLoadFav.y = tmpY;
                            } else if (tmpScreen == sMaxPosAfterLoadFav.s) {
                                if (tmpY > sMaxPosAfterLoadFav.y) {
                                    sMaxPosAfterLoadFav.x = tmpX;
                                    sMaxPosAfterLoadFav.y = tmpY;
                                } else if (tmpY == sMaxPosAfterLoadFav.y) {
                                    if (tmpX > sMaxPosAfterLoadFav.x) {
                                        sMaxPosAfterLoadFav.x = tmpX;
                                    }
                                }
                            }
                            if (TAG_FAVORITE.equals(folder_item_name) && folderId >= 0) {
                                long id =
                                    addAppShortcut(db, values, ar, packageManager, intent);
                                if (id >= 0) {
                                    folderItems.add(id);
                                }
                            } else if (TAG_SHORTCUT.equals(folder_item_name) && folderId >= 0) {
                                long id = addUriShortcut(db, values, ar);
                                if (id >= 0) {
                                    folderItems.add(id);
                                }
                            } else {
                                throw new RuntimeException("Folders can " +
                                        "contain only shortcuts");
                            }
                            ar.recycle();
                        }
                        // We can only have folders with >= 2 items, so we need to remove the
                        // folder and clean up if less than 2 items were included, or some
                        // failed to add, and less than 2 were actually added
                        if (folderItems.size() < 2 && folderId >= 0) {
                            // We just delete the folder and any items that made it
                            deleteId(db, folderId);
                            if (folderItems.size() > 0) {
                                deleteId(db, folderItems.get(0));
                            }
                            added = false;
                        }
                    }else if (TAG_ALIAPPWIDGET.equals(name)) {
                    	added = addAliAppWidget(parser, attrs, type, db, values, a, packageManager);
                        /* YUNOS BEGIN */
                        // ##gadget
                        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com
                        // ##BugID:96378
                    }else if (TAG_GADGET.equals(name)) {
                        added = addGadget(parser, attrs, type, db, values, a, packageManager);
                        /* YUNOS END */
                    }
                    if (added) i++;
                    a.recycle();
                }
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            } catch (IOException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            } catch (RuntimeException e) {
                Log.w(TAG, "Got exception parsing favorites.", e);
            }
            return i;
        }

        private long addAppShortcut(SQLiteDatabase db, ContentValues values, TypedArray a,
                PackageManager packageManager, Intent intent) {
            long id = -1;
            ActivityInfo info;
            String packageName = a.getString(R.styleable.Favorite_packageName);
            String className = a.getString(R.styleable.Favorite_className);
            try {
                ComponentName cn;
                try {
                    cn = new ComponentName(packageName, className);
                    info = packageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    String[] packages = packageManager.currentToCanonicalPackageNames(
                        new String[] { packageName });
                    cn = new ComponentName(packages[0], className);
                    info = packageManager.getActivityInfo(cn, 0);
                }

                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                values.put(Favorites.INTENT, intent.toUri(0));

                values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPLICATION);
                values.put(Favorites.SPANX, 1);
                values.put(Favorites.SPANY, 1);

                /*YUNOS BEGIN*/
                //##date:2014/6/4 ##author:zhangqiang.zq
                // smart search
                String title = info.loadLabel(packageManager).toString();
                values.put(Favorites.TITLE, title);

                updateTitlePinyin(values, title);
                /*YUNOS END*/
                id = generateNewId();
                values.put(Favorites._ID, id);
                if (dbInsertAndCheck(this, db, mCurrentTable, null, values) < 0) {
                    return -1;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to add favorite: " + packageName +
                        "/" + className, e);
            }
            return id;
        }

        private long addFolder(SQLiteDatabase db, ContentValues values) {
            values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_FOLDER);
            values.put(Favorites.SPANX, 1);
            values.put(Favorites.SPANY, 1);
            long id = generateNewId();
            values.put(Favorites._ID, id);
            if (dbInsertAndCheck(this, db, mCurrentTable, null, values) <= 0) {
                return -1;
            } else {
                return id;
            }
        }

        private ComponentName getSearchWidgetProvider() {
            SearchManager searchManager =
                    (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
            ComponentName searchComponent = searchManager.getGlobalSearchActivity();
            if (searchComponent == null) return null;
            return getProviderInPackage(searchComponent.getPackageName());
        }

        /**
         * Gets an appwidget provider from the given package. If the package contains more than
         * one appwidget provider, an arbitrary one is returned.
         */
        private ComponentName getProviderInPackage(String packageName) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
            List<AppWidgetProviderInfo> providers = appWidgetManager.getInstalledProviders();
            if (providers == null) return null;
            final int providerCount = providers.size();
            for (int i = 0; i < providerCount; i++) {
                ComponentName provider = providers.get(i).provider;
                if (provider != null && provider.getPackageName().equals(packageName)) {
                    return provider;
                }
            }
            return null;
        }

        private boolean addSearchWidget(SQLiteDatabase db, ContentValues values) {
            ComponentName cn = getSearchWidgetProvider();
            return addAppWidget(db, values, cn, 4, 1, null);
        }

        private boolean addClockWidget(SQLiteDatabase db, ContentValues values) {
            ComponentName cn = new ComponentName("com.android.alarmclock",
                    "com.android.alarmclock.AnalogAppWidgetProvider");
            return addAppWidget(db, values, cn, 2, 2, null);
        }

        private boolean addAppWidget(XmlResourceParser parser, AttributeSet attrs, int type,
                SQLiteDatabase db, ContentValues values, TypedArray a,
                PackageManager packageManager) throws XmlPullParserException, IOException {

            String packageName = a.getString(R.styleable.Favorite_packageName);
            String className = a.getString(R.styleable.Favorite_className);

            if (packageName == null || className == null) {
                return false;
            }

            boolean hasPackage = true;
            ComponentName cn = new ComponentName(packageName, className);
            try {
                packageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = packageManager.currentToCanonicalPackageNames(
                        new String[] { packageName });
                cn = new ComponentName(packages[0], className);
                try {
                    packageManager.getReceiverInfo(cn, 0);
                } catch (Exception e1) {
                    hasPackage = false;
                }
            }

            if (hasPackage) {
                int spanX = a.getInt(R.styleable.Favorite_spanX, 0);
                int spanY = a.getInt(R.styleable.Favorite_spanY, 0);

                // Read the extras
                Bundle extras = new Bundle();
                int widgetDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > widgetDepth) {
                    if (type != XmlPullParser.START_TAG) {
                        continue;
                    }

                    TypedArray ar = mContext.obtainStyledAttributes(attrs, R.styleable.Extra);
                    if (TAG_EXTRA.equals(parser.getName())) {
                        String key = ar.getString(R.styleable.Extra_key);
                        String value = ar.getString(R.styleable.Extra_value);
                        if (key != null && value != null) {
                            extras.putString(key, value);
                        } else {
                            throw new RuntimeException("Widget extras must have a key and value");
                        }
                    } else {
                        throw new RuntimeException("Widgets can contain only extras");
                    }
                    ar.recycle();
                }

                return addAppWidget(db, values, cn, spanX, spanY, extras);
            }

            return false;
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues values, ComponentName cn,
                int spanX, int spanY, Bundle extras) {
            boolean allocatedAppWidgets = false;
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);

            try {
                int appWidgetId = mAppWidgetHost.allocateAppWidgetId();

                //add by wenliang.dwl, need Intent when backup and restore
                Intent intentDescription = new Intent();
                intentDescription.setComponent(cn);
                values.put(Favorites.INTENT, intentDescription.toUri(0));

                values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPWIDGET);
                values.put(Favorites.SPANX, spanX);
                values.put(Favorites.SPANY, spanY);
                values.put(Favorites.APPWIDGET_ID, appWidgetId);
                values.put(Favorites._ID, generateNewId());
                dbInsertAndCheck(this, db, mCurrentTable, null, values);

                allocatedAppWidgets = true;

                // TODO: need to check return value
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);

                // Send a broadcast to configure the widget
                if (extras != null && !extras.isEmpty()) {
                    Intent intent = new Intent(ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE);
                    intent.setComponent(cn);
                    intent.putExtras(extras);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    mContext.sendBroadcast(intent);
                }
            } catch (RuntimeException ex) {
                Log.e(TAG, "Problem allocating appWidgetId", ex);
            }

            return allocatedAppWidgets;
        }

        private long addUriShortcut(SQLiteDatabase db, ContentValues values,
                TypedArray a) {
            Resources r = mContext.getResources();

            final int iconResId = a.getResourceId(R.styleable.Favorite_icon, 0);
            final int titleResId = a.getResourceId(R.styleable.Favorite_title, 0);

            Intent intent;
            String uri = null;
            try {
                uri = a.getString(R.styleable.Favorite_uri);
                intent = Intent.parseUri(uri, 0);
            } catch (URISyntaxException e) {
                Log.w(TAG, "Shortcut has malformed uri: " + uri);
                return -1; // Oh well
            }

            if (iconResId == 0 || titleResId == 0) {
                Log.w(TAG, "Shortcut is missing title or icon resource ID");
                return -1;
            }

            long id = generateNewId();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            values.put(Favorites.INTENT, intent.toUri(0));
            values.put(Favorites.TITLE, r.getString(titleResId));
            values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_SHORTCUT);
            values.put(Favorites.SPANX, 1);
            values.put(Favorites.SPANY, 1);
            values.put(Favorites.ICON_TYPE, Favorites.ICON_TYPE_RESOURCE);
            values.put(Favorites.ICON_PACKAGE, mContext.getPackageName());
            values.put(Favorites.ICON_RESOURCE, r.getResourceName(iconResId));
            values.put(Favorites._ID, id);

            if (dbInsertAndCheck(this, db, mCurrentTable, null, values) < 0) {
                return -1;
            }
            return id;
        }
    
    //edit by dongjun for traffic panel begin
    private boolean addAliAppWidget(XmlResourceParser parser, AttributeSet attrs, int type,
            SQLiteDatabase db, ContentValues values, TypedArray a,
            PackageManager packageManager) throws XmlPullParserException, IOException{
    	String title = a.getString(R.styleable.Favorite_title);
        if (title == null){
        	Log.e(TAG,"got one fail myappwidget");
            return false;
        }

        int x = a.getInt(R.styleable.Favorite_x, 0);
        int y = a.getInt(R.styleable.Favorite_y, 0);
        int spanX = a.getInt(R.styleable.Favorite_spanX, 0);
        int spanY = a.getInt(R.styleable.Favorite_spanY, 0);

        // Read the extras
        Bundle extras = new Bundle();
        int widgetDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > widgetDepth) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            TypedArray ar = mContext.obtainStyledAttributes(attrs, R.styleable.Extra);
            if (TAG_EXTRA.equals(parser.getName())) {
                String key = ar.getString(R.styleable.Extra_key);
                String value = ar.getString(R.styleable.Extra_value);
                if (key != null && value != null) {
                    extras.putString(key, value);
                } else {
                    throw new RuntimeException("Widget extras must have a key and value");
                }
            } else {
                throw new RuntimeException("Widgets can contain only extras");
            }
            ar.recycle();
        }
        
        boolean allocatedAppWidgets = false;
        try {
            values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_ALIAPPWIDGET);
            values.put(Favorites.CELLX, x);
            values.put(Favorites.CELLY, y);
            values.put(Favorites.SPANX, spanX);
            values.put(Favorites.SPANY, spanY);
            values.put(Favorites.TITLE, title);
            values.put(Favorites._ID, generateNewId());
            dbInsertAndCheck(this, db, mCurrentTable, null, values);
            allocatedAppWidgets = true;

        } catch (Exception ex) {
            Log.w(TAG, "Problem allocating appWidgetId", ex);
        }
        return allocatedAppWidgets;
    }
    //edit by dongjun for traffic panel begin
        /* YUNOS BEGIN */
        // ##gadget
        // ##date:2014/02/27 ##author:kerong.skr@alibaba-inc.com ##BugID:96378
    private boolean addGadget(XmlResourceParser parser, AttributeSet attrs, int type,
            SQLiteDatabase db, ContentValues values, TypedArray a,
            PackageManager packageManager) throws XmlPullParserException, IOException{
        String title = a.getString(R.styleable.Favorite_title);
        if (title == null){
            Log.e(TAG,"got one fail myappwidget");
            return false;
        }

        int x = a.getInt(R.styleable.Favorite_x, 0);
        int y = a.getInt(R.styleable.Favorite_y, 0);
        int spanX = a.getInt(R.styleable.Favorite_spanX, 0);
        int spanY = a.getInt(R.styleable.Favorite_spanY, 0);

        // Read the extras
        Bundle extras = new Bundle();
        int widgetDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_TAG ||
                parser.getDepth() > widgetDepth) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            TypedArray ar = mContext.obtainStyledAttributes(attrs, R.styleable.Extra);
            if (TAG_EXTRA.equals(parser.getName())) {
                String key = ar.getString(R.styleable.Extra_key);
                String value = ar.getString(R.styleable.Extra_value);
                if (key != null && value != null) {
                    extras.putString(key, value);
                } else {
                    throw new RuntimeException("Widget extras must have a key and value");
                }
            } else {
                throw new RuntimeException("Widgets can contain only extras");
            }
            ar.recycle();
        }
        
        boolean allocatedAppWidgets = false;
        try {
            values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_GADGET);
            values.put(Favorites.CELLX, x);
            values.put(Favorites.CELLY, y);
            values.put(Favorites.SPANX, spanX);
            values.put(Favorites.SPANY, spanY);
            values.put(Favorites.TITLE, title);
            values.put(Favorites._ID, generateNewId());
            dbInsertAndCheck(this, db, mCurrentTable, null, values);
            allocatedAppWidgets = true;

        } catch (Exception ex) {
            Log.w(TAG, "Problem allocating appWidgetId", ex);
        }
        return allocatedAppWidgets;
    }
        /* YUNOS END */
}

    /**
     * Build a query string that will match any row where the column matches
     * anything in the values list.
     */
    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                //BugID:5595776:backup normal mode favorites table
                if (url.getPathSegments().get(0).equals(NORMAL_TABLE_FAVORITES)) {
                    this.table = TABLE_FAVORITES;
                } else if (url.getPathSegments().get(0).equals(TABLE_AGED_FAVORITES)) {
                    this.table = TABLE_AGED_FAVORITES;
                } else {
                    this.table = mDbAgedMode ? TABLE_AGED_FAVORITES : url.getPathSegments().get(0);
                }

                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                /* YUNOS BEGIN */
                // ##date:2015/1/17 ##author:yangshan.ys##5675067
                // the provider callers can set the specifies the explicit table
                // by path params
                if (url.getPathSegments().get(0).equals(NORMAL_TABLE_FAVORITES)) {
                    this.table = TABLE_FAVORITES;
                } else if (url.getPathSegments().get(0).equals(TABLE_AGED_FAVORITES)) {
                    this.table = TABLE_AGED_FAVORITES;
                } else {
                    this.table = mDbAgedMode ? TABLE_AGED_FAVORITES : url.getPathSegments().get(0);
                }
                /* YUNOS END */
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = mDbAgedMode ? TABLE_AGED_FAVORITES : url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

    /* YUNOS BEGIN */
    //##date:2013/11/28 ##author:hongxing.whx ##bugid: 67654
    public static void clearLoadDefaultWorkspaceFlag() {
        Log.d(TAG, "clearLoadDefaultWorkspaceFlag");
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = LauncherApplication.getContext().getSharedPreferences(spKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        /*YUNOS BEGIN*/
        //##date:2014/7/8 ##author:zhangqiang.zq
        // aged mode
        if (mDbAgedMode) {
            editor.remove(DB_CREATED_BUT_AGED_WORKSPACE_NOT_LOADED);
        } else {
            editor.remove(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED);
        }
        /*YUNOS END*/
        editor.commit();
    }
    /* YUNOS END */

    /*YUNOS BEGIN*/
    //##date:2014/5/22 ##author:zhangqiang.zq
    // smart search
    //##date:2015/2/2 ##author:zhanggong.zg ##BugID:5719824
    // smart search is disabled to reduce initial memory usage
    public static void updateTitlePinyin(ContentValues values, String title) {
        /*
        List<List<String>> pyList = HanziToPinyin.getInstance().getHanziPinyin(title);
        StringBuilder full = new StringBuilder();
        StringBuilder shortn = new StringBuilder();
        boolean addSeperator = false;
        for (List<String> py : pyList) {
            if (addSeperator) {
                    full.append(";");
                    shortn.append(";");
                }
            for (int i = 0; i < py.size(); i++) {
                boolean first = true;
                char[] pinyin = py.get(i).trim().toLowerCase().toCharArray();
                for (char c : pinyin) {
                    char num = (c >= 'a' && c <= 'z') ? HanziToPinyin.Data_Letters_To_T9[c - 'a'] : c;
                    if (first) {
                        shortn.append(num);
                        first = false;
                    }
                full.append(num);
                }
            }
            addSeperator = true;
        }
        values.put(LauncherSettings.BaseLauncherColumns.FULL_PINYIN, full.toString());
        values.put(LauncherSettings.BaseLauncherColumns.SHORT_PINYIN, shortn.toString());
        */
    }
    /*YUNOS END*/
}
