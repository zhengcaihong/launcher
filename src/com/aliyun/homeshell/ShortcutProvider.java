package com.aliyun.homeshell;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class ShortcutProvider extends ContentProvider{
    private static final String TAG = "ShortcutProvider";
    private static final UriMatcher URI_MATCHER;
    private static final int URL_MATCH_ALL  = 0;
    private static final int URI_MATCH_CLOUDAPP = 1;
    private static final int URI_MATCH_BOOKMARK = 2;

    //path
    static final String ALL = "all";
    static final String BOOKMARK = "bookmark";
    static final String CLOLDAPP = "cloudapp";

    private static final String AUTHORITY = "com.aliyun.homeshell.shortcut";
    public static final Uri CONTENT_BOOKMARK = Uri.parse(
            "content://" + AUTHORITY + "/" + BOOKMARK);
    public static final Uri CONTENT_CLOUDAPP = Uri.parse(
            "content://" + AUTHORITY + "/" + CLOLDAPP);

    private SQLiteOpenHelper mOpenHelper;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, ALL,
                URL_MATCH_ALL);
        URI_MATCHER.addURI(AUTHORITY, BOOKMARK,
                URI_MATCH_BOOKMARK);
        URI_MATCHER.addURI(AUTHORITY, CLOLDAPP,
                URI_MATCH_CLOUDAPP);
    }

    @Override
    public boolean onCreate() {
        try {
            mOpenHelper = new LauncherProvider.DatabaseHelper(getContext());
            return false;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in onCreate : throw SQLiteFullException");
            throw e;
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in onCreate : " + e.getMessage());
        }
        
        return false;
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
        int match = URI_MATCHER.match(uri);

        switch(match){
        case URL_MATCH_ALL:{
            String andWhere = LauncherSettings.Favorites.ITEM_TYPE + "="
                    + LauncherSettings.Favorites.ITEM_TYPE_CLOUDAPP + " OR " +
                        LauncherSettings.Favorites.ITEM_TYPE + "="
                    + LauncherSettings.Favorites.ITEM_TYPE_BOOKMARK;
            if(selection == null){
                selection = andWhere;
            }else{
                selection = selection + " AND (" + andWhere +") ";
            }
            break;
        }
        case URI_MATCH_CLOUDAPP:{
            String andWhere = LauncherSettings.Favorites.ITEM_TYPE + "="
                    + LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
            if(selection == null){
                selection = andWhere;
            }else{
                selection = selection + " AND (" + andWhere +") ";
            }
            break;
        }
        case URI_MATCH_BOOKMARK:{
            String andWhere = LauncherSettings.Favorites.ITEM_TYPE + "="
                    + LauncherSettings.Favorites.ITEM_TYPE_BOOKMARK;
            if(selection == null){
                selection = andWhere;
            }else{
                selection = selection + " AND (" + andWhere +") ";
            }
            break;
        }

        default:
            return null;
        }

        try {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            qb.setTables(LauncherProvider.TABLE_FAVORITES);
            if (mOpenHelper == null) {
                mOpenHelper = new LauncherProvider.DatabaseHelper(getContext());
            }

            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final Cursor result = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
            result.setNotificationUri(getContext().getContentResolver(), uri);
    
            return result;
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Failed in query : throw SQLiteFullException");
            throw e;
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed in query : " + e.getMessage());
        }

        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
