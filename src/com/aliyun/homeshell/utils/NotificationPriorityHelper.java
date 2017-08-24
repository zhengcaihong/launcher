package com.aliyun.homeshell.utils;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class NotificationPriorityHelper extends ContentObserver {

    public static final String TAG = "NotificationPriorityHelper";
    public static final String PKG_COLUMN = "package_name";
    public static final String PRI_COLUMN = "priority";
    public static final Uri URI = Uri.parse("content://systemui-provider/package");

    private static NotificationPriorityHelper sInstance = null;

    public static NotificationPriorityHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NotificationPriorityHelper(context);
        }
        return sInstance;
    }

    public static void destroy() {
        if (sInstance != null) {
            sInstance.onDestroy();
            sInstance = null;
        }
    }

    private Context mContext;
    private Map<String, Integer> mMap;

    private NotificationPriorityHelper(Context context) {
        super(new Handler());
        mContext = context.getApplicationContext();
        mMap = new HashMap<String, Integer>();
        mContext.getContentResolver().registerContentObserver(URI, true, this);
        queryPackagePriorities();
    }

    private void queryPackagePriorities() {
        Log.d(TAG, "queryPackagePriorities");
        Cursor c = null;
        String[] projection = new String[] { PKG_COLUMN, PRI_COLUMN };
        try {
            mMap.clear();
            c = mContext.getContentResolver().query(URI, projection, null, null, null);
            if (c != null && c.moveToFirst()) {
                int priIndex = c.getColumnIndex(PRI_COLUMN);
                int pkgIndex = c.getColumnIndex(PKG_COLUMN);
                do {
                    String pkg = c.getString(pkgIndex);
                    int pri = c.getInt(priIndex);
                    mMap.put(pkg, pri);
                } while (c.moveToNext());
            } else {
                Log.d(TAG, "queryPackagePriorities Cursor is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "queryPackagePriorities Exception is ",e);
        } finally {
            if (c != null)
                c.close();
        }
    }

    /*
     * if this package's notifications is important to user
     */
    public boolean isPkgImportant(String pkg) {
        Integer pri = mMap.get(pkg);
        Log.d(TAG,"isPkgImportant pkg "+pkg+" is "+(pri==null?0:pri));
        if( pri == null ) return false;
        return pri == 1;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        queryPackagePriorities();
    }

    private void onDestroy() {
        mContext.getContentResolver().unregisterContentObserver(this);
        mMap.clear();
    }
}
