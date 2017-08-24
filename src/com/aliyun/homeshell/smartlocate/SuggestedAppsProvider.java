package com.aliyun.homeshell.smartlocate;

import java.util.List;

import com.aliyun.homeshell.ShortcutInfo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SuggestedAppsProvider extends ContentProvider {

    private static final String TAG = "SuggestedAppsProvider";

    private static final String AUTHORITY = "com.aliyun.homeshell.suggestedapps";
    private static final int COUNT = 1;
    public static final String COUNT_TYPE = "com.aliyun.homeshell.suggestedapps/count";

    private static final int LIMIT = 8;

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_INTENT = "intentDescription";
    public static final String COLUMN_CARDMODE = "isCardIcon";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_USERID = "userId";

    private static UriMatcher uriMatcher;
    private static String[] columnNames;

    @Override
    public boolean onCreate() {
        return true;
    }

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "count", COUNT);
        columnNames = new String[6];
        columnNames[0] = COLUMN_ID;
        columnNames[1] = COLUMN_TITLE;
        columnNames[2] = COLUMN_INTENT;
        columnNames[3] = COLUMN_CARDMODE;
        columnNames[4] = COLUMN_ICON;
        /* YUNOS BEGIN */
	    // ## date: 2016/06/27 ## author: yongxing.lyx
	    // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
        columnNames[5] = COLUMN_USERID;
        /* YUNOS END */
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        boolean onlyCount = false;
        switch (uriMatcher.match(uri)) {
            case COUNT :
                onlyCount = true;
                break;

            default :
                onlyCount = false;
                break;
        }
        int limit = LIMIT;
        if (!TextUtils.isEmpty(sortOrder)) {
            try {
                limit = Integer.parseInt(sortOrder);
            } catch (NumberFormatException e) {
                limit = LIMIT;
            }
        }
        List<ShortcutInfo> suggestedApps = SuggestedApps.getSuggestedApps(getContext(), limit);
        Log.d(TAG, "SuggestedAppsCursor   query  uri  " + uri + " onlyCount " + onlyCount + " limit " + limit + " suggestedApps "
                + suggestedApps);

        Cursor cursor = new SuggestedAppsCursor(columnNames, suggestedApps, onlyCount);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
