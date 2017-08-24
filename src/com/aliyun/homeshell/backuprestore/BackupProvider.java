
package com.aliyun.homeshell.backuprestore;

import java.net.URISyntaxException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.aliyun.homeshell.AppUtil;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;

public class BackupProvider extends ContentProvider {
    private static final String TAG = "BackupProvider";
    private static final Uri BACKUP_URI = Uri
            .parse("content://com.aliyun.homeshell.externalprovider/backup");
    public static final String DIVIDER = "com.aliyun.homeshell.divider";
    ContentResolver mResolver = null;

    @Override
    public boolean onCreate() {
        mResolver = this.getContext().getContentResolver();
        return true;
    }

    // interface for supplying data to backup
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.d(TAG, "query begin");
        if (BACKUP_URI.equals(uri)) {
            Cursor curDB = mResolver.query(LauncherSettings.Favorites.CONTENT_URI, null, null,
                    null, null);
            Cursor curDivder = getDividerCursor(curDB);
            Cursor curAppList = getApplistCursor(curDB);
            Cursor cursors[] = new Cursor[] {
                    curDB,
                    curDivder,
                    curAppList
            };

            MergeCursor resultCursor = new MergeCursor(cursors);
            return resultCursor;
        }
        Log.d(TAG, "invalid uri");
        return null;
    }

    private Cursor getDividerCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        String[] project = cursor.getColumnNames();
        MatrixCursor c = new MatrixCursor(project);
        c.addRow(new
                Object[] {
                        0, DIVIDER, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null,
                        null, null, null, null, null, 0, 0, 0, null, null, 0, null
                });
        return c;
    }

    private Cursor getApplistCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        String[] project = cursor.getColumnNames();
        MatrixCursor firstcursor = new MatrixCursor(project);
        MatrixCursor appcursor = new MatrixCursor(project);
        SparseArray<AppInfo> firstApps = new SparseArray<AppInfo>();

        int titleIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
        int intentIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
        int containerIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
        int cellXIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        int cellYIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        int cellXLandIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLXLAND);
        int cellYLandIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLYLAND);
        int screeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
        int typeIndex = cursor.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);

        cursor.moveToFirst();

        String title;
        String intent;
        int container;
        int screen;
        int cellX = -1;
        int cellY = -1;
        int cellXLand = -1;
        int cellYLand = -1;
        int type;

        do {
            title = cursor.getString(titleIndex);
            intent = cursor.getString(intentIndex);
            container = cursor.getInt(containerIndex);
            screen = cursor.getInt(screeIndex);
            cellX = cursor.getInt(cellXIndex);
            cellY = cursor.getInt(cellYIndex);
            cellXLand = cursor.getInt(cellXLandIndex);
            cellYLand = cursor.getInt(cellYLandIndex);
            type = cursor.getInt(typeIndex);

            AppInfo app = new AppInfo(title, intent, container, screen, cellX, cellY, type);
            app.mCellXLand = cellXLand;
            app.mCellYLand = cellYLand;
            if (!app.isThirdApp()) {
                continue;
            }

            AppInfo first = (AppInfo) firstApps.get(app.mScreen);
            if(first != null){
                AppInfo temp = app;
                if (app.isBefore(first)) {
                    firstApps.put(app.mScreen, app);
                    temp = first;
                }
                appcursor.addRow(temp.toValue());
            }else{
                firstApps.put(app.mScreen, app);
            }
        } while (cursor.moveToNext());

        for(int i =0;i< firstApps.size();i++){
            firstcursor.addRow(firstApps.valueAt(i).toValue());
        }

        return new MergeCursor(new Cursor[] {
                firstcursor, appcursor
        });
    }

    // interface for getting datat from backup
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        Log.d(TAG, "bulkInsert");
        if (BACKUP_URI.equals(uri)) {
            BackupAgent agent = new BackupAgent();
            agent.restoreDB(values, 0);
            agent = null;
            return 1;
        } else {
            Log.d(TAG, "invalid uri");
            return 0;
        }
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    // the provider only supply query and applyBatch for backup, so insert do
    // noting
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    // the provider only supply query and applyBatch for backup, so delete do
    // noting
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    // the provider only supply query and applyBatch for backup, so update do
    // noting
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

//    private void dumpCursorInfo(Cursor cursor) {
//        int titleid = cursor.getColumnIndexOrThrow("title");
//        int intentid = cursor.getColumnIndexOrThrow("intent");
//        String title = "";
//        String intent = "";
//        StringBuilder info = new StringBuilder();
//        if (cursor != null) {
//            cursor.moveToFirst();
//            do {
//                title = cursor.getString(titleid);
//                intent = cursor.getString(intentid);
//                info.append("{" + title + "," + intent + "}" + "\n");
//            } while (cursor.moveToNext());
//        }
//        Log.d(TAG, "dumpInfo : info = " + info);
//    }

    public static class AppInfo {
        public String mIntent;
        public String mTitle;
        public int mContainer;
        public int mScreen;
        public int mCellX;
        public int mCellY;
        public int mCellXLand;
        public int mCellYLand;
        public int mType;

        public AppInfo(String title, String intent, int container, int screen, int cellX,
                int cellY, int type) {
            mIntent = intent;
            mTitle = title;
            mContainer = container;
            mScreen = screen;
            mCellX = cellX;
            mCellY = cellY;
            mType = type;
        }

        public boolean isBefore(AppInfo app) {
            boolean before = false;
            if (mCellY < app.mCellY) {
                before = true;
            } else if (mCellY == app.mCellY && mCellX < app.mCellX) {
                before = true;
            }
            return before;
        }

        @SuppressWarnings("finally")
        public boolean isThirdApp() {
            boolean third = false;
            try {
                String pkgname = Intent.parseUri(mIntent, 0).getComponent().getPackageName();
                if (mContainer == LauncherSettings.Favorites.CONTAINER_DESKTOP
                        && mType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                        && !AppUtil.isSystemApp(LauncherApplication.getContext(), pkgname))
                    third = true;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } finally {
                return third;
            }
        }

        public Object[] toValue() {
            return new Object[] {
                    0, mTitle, mIntent, mContainer, mScreen, mCellX, mCellY, mCellXLand, mCellYLand, 0, 0, 0, 0, 0, null,
                    null, null, null, null, null, 0, 0, 0, null, null, 0, null
            };
        }
    }
}
