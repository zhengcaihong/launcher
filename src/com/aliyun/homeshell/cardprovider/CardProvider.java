package com.aliyun.homeshell.cardprovider;

import java.util.List;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 *
 * @author qiang
 * This provider is a temp provider.
 * Only for homeshell card using!!!
 *
 */
public class CardProvider extends ContentProvider {

    private static final String TAG = "CardProvider";
    private static final String TABLE_MMS = "mms";
    private static final String TABLE_CALLS = "calls";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        List<String> path = uri.getPathSegments();
        String tableName = path.get(0);
        if (TABLE_MMS.equals(tableName)) {
            return QueryFactory.getQuery(QueryFactory.TYPE_MMS)
                    .doQuery(getContext(), projection, selection, selectionArgs, sortOrder);
        } else if (TABLE_CALLS.equals(tableName)) {
            return QueryFactory.getQuery(QueryFactory.TYPE_CALLS)
                    .doQuery(getContext(), projection, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        Log.e(TAG, "getType: not support", new Throwable());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e(TAG, "insert: not support", new Throwable());
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e(TAG, "delete: not support", new Throwable());
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        Log.e(TAG, "update: not support", new Throwable());
        return 0;
    }
}
