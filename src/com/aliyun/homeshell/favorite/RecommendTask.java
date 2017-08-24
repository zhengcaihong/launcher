package com.aliyun.homeshell.favorite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.aliyun.homeshell.FastBitmapDrawable;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.LauncherSettings;
import com.aliyun.homeshell.LauncherSettings.Favorites;
import com.aliyun.homeshell.icon.IconManager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class RecommendTask {

    private static final String TAG = "RecommendTask";

    private HandlerThread mThread;
    private Handler mHandler;
    private Context mContext;
    private RecommendDatabaseHelper mRecommendDatabaseHelper;

    private static final int[] DAYS_OF_MONTH = {31, 28, 31,
                            30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int FAVORITE_APP_COUNT = 4;

    private List<Long> mFavoriteAppIds;
    private static IconManager mIconManager;

    private static final boolean SUPPORT_DB_FAVORATE_ICON = false;

    public RecommendTask(Context context) {
        mThread = new HandlerThread("SearchThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());

        mContext = context;
        mRecommendDatabaseHelper = new RecommendDatabaseHelper(mContext);
        mFavoriteAppIds = new ArrayList<Long>();

        if (mIconManager == null) {
            mIconManager = ((LauncherApplication)mContext
                    .getApplicationContext()).getIconManager();
        }
    }

    public void notifyAppClicked(long id) {
        RecommendRunnable r = new RecommendRunnable();
        r.setData(id);
        mHandler.post(r);
    }

    private class RecommendRunnable implements Runnable {
        private long mId = -1;

        void setData(long id) {
            mId = id;
        }

        private void updateRecorder(SQLiteDatabase db) {
            int date = getCurrentDate();
            long time = getCurrentTime();
            Cursor c = db.query(RecommendDatabaseHelper.APP_RECORDE_TABLE,
                    new String[]{RecommendDatabaseHelper.CLICK_COUNT},
                    RecommendDatabaseHelper.APP_ID + "=" + mId
                    + " AND " + RecommendDatabaseHelper.DATE + "=" + date,
                    null, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int count = c.getInt(0) + 1;
                ContentValues value = new ContentValues();
                value.put(RecommendDatabaseHelper.CLICK_COUNT, count);
                value.put(RecommendDatabaseHelper.CLICK_TIME, time);
                db.update(RecommendDatabaseHelper.APP_RECORDE_TABLE,
                        value, RecommendDatabaseHelper.APP_ID + "=" + mId
                        + " AND " + RecommendDatabaseHelper.DATE + "=" + date, null);
            } else {
                ContentValues value = new ContentValues();
                value.put(RecommendDatabaseHelper.APP_ID, mId);
                value.put(RecommendDatabaseHelper.DATE, date);
                value.put(RecommendDatabaseHelper.CLICK_COUNT, 1);
                value.put(RecommendDatabaseHelper.CLICK_TIME, time);
                db.insert(RecommendDatabaseHelper.APP_RECORDE_TABLE,
                        RecommendDatabaseHelper.APP_ID, value);
            }

            if (c != null) {
                c.close();
            }
        }

        @Override
        public void run() {
            SQLiteDatabase db = null;
            try {
                db = mRecommendDatabaseHelper.getWritableDatabase();
                if (mId >= 0) {
                    updateRecorder(db);
                }
                updateHomeshellDb(db);
                gcDatabase(db);
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed at getWritableDatabase : " + e.getMessage());
            } catch (Exception e) {
                Log.w(TAG, "Error Happened!!!" + e.getMessage());
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }
    }

    private static int getCurrentDate() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int dates = day;
        for (int i = 0; i < month; i++) {
            dates += DAYS_OF_MONTH[i];
        }

        dates += (year - 2000) * 365 + (year - 2000) / 4
                - (year - 2000) / 100 + (year - 2000) / 400;

        if (((year % 400 == 0) || (year % 4 == 0
                && year % 100 != 0)) && month < 3) {
            dates --;
        }
        return dates;
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

    private void fetchUnifiedIcon(long id, ContentValues values) {
        Cursor c = mContext.getContentResolver().query(Favorites.CONTENT_URI,
                new String[]{Favorites.INTENT, Favorites.FAVORITE_ICON},
                LauncherSettings.Favorites._ID + "=" + id, null, null);
        Intent intent = null;
        if (c != null && c.getCount() > 0) {
            c.moveToPosition(0);
            byte[] icon = c.getBlob(1);
            if (icon != null) {
                c.close();
                return;
            }
            String intentDesc = c.getString(0);
            if (intentDesc != null && !intentDesc.isEmpty()) {
                try {
                    intent = Intent.parseUri(intentDesc, 0);
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Parser intent error!!!:" + intentDesc);
                }
            }
        }

        if (c != null) {
            c.close();
        }

        if (intent != null) {
            Drawable icon = mIconManager.getAppUnifiedIcon(intent);
            if (icon != null) {
                writeBitmap(values, icon);
            }
        }
    }

    private static void gcDatabase(SQLiteDatabase db) {
        int date = getCurrentDate();
        String sql = "delete from app_recoder where date <"
                        + (date - 45) + " or date >" + (date + 7);
        db.execSQL(sql);
    }

    private static Cursor getRecommendFavoriteApps(SQLiteDatabase db,
            int dateStart, int dateEnd) {
        int date = getCurrentDate();
        String sql = "select appId,sum(clickCount) as countSum, max(clickTime) as maxTime"
                        + " from app_recoder where date >"
                        + (date - dateStart - 1) + " and date <" 
                        + (date + dateEnd + 1)
                        + " group by appId order by countSum desc, maxTime desc";
        return db.rawQuery(sql, null);
    }

    private static byte[] flattenBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write icon");
            return null;
        }
    }

    private static void writeBitmap(ContentValues values, Drawable bitmap) {
        if (bitmap != null) {
            Bitmap src = null;
            if (bitmap instanceof FastBitmapDrawable) {
                src = ((FastBitmapDrawable) bitmap).getBitmap();
            } else {
                src = ((BitmapDrawable) bitmap.getCurrent()).getBitmap();
            }
            writeBitmap(values,src);
        }
    }

    private static void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put(LauncherSettings.Favorites.FAVORITE_ICON, data);
        }
    }

    public void refreshFavoriteAppIcons() {
        if (SUPPORT_DB_FAVORATE_ICON) {
            ((LauncherApplication)mContext.getApplicationContext())
                .getLauncherProvider().clearFavoriteIcons();
            notifyAppClicked(-1);
        }
    }

    private void updateHomeshellDb(SQLiteDatabase db) {
        List<Long> newFavorites = getFavoriteApps(db);

        ContentValues values = new ContentValues();
        for (long id : mFavoriteAppIds) {
            values.clear();
            values.put(LauncherSettings.Favorites.FAVORITE_WEIGHT, 0);
            mContext.getContentResolver().update(Favorites.CONTENT_URI_NO_NOTIFICATION, values,
                    LauncherSettings.Favorites._ID + "=" + id, null);
        }
        mFavoriteAppIds.clear();

        int weight = newFavorites.size();
        for (long id : newFavorites) {
            values.clear();
            if (SUPPORT_DB_FAVORATE_ICON) {
                fetchUnifiedIcon(id, values);
            }
            values.put(LauncherSettings.Favorites.FAVORITE_WEIGHT, weight--);
            mContext.getContentResolver().update(Favorites.CONTENT_URI_NO_NOTIFICATION, values,
                    LauncherSettings.Favorites._ID + "=" + id, null);
            mFavoriteAppIds.add(id);
        }
    }

    private  List<Long> getFavoriteApps(SQLiteDatabase db) {
        List<Long> newFavorites = new ArrayList<Long>();
        refreshFavoriteApps(newFavorites, db, 7, 0);

        if (newFavorites.size() < FAVORITE_APP_COUNT) {
            refreshFavoriteApps(newFavorites, db, 14, 7);
        }

        if (newFavorites.size() < FAVORITE_APP_COUNT) {
            refreshFavoriteApps(newFavorites, db, 28, 14);
        }
        return newFavorites;
    }

    private void refreshFavoriteApps(List<Long> newFavorites,
            SQLiteDatabase db, int startDate, int endDate) {
        Cursor favorite = getRecommendFavoriteApps(db, startDate, endDate);
        if (favorite != null) {
            int count = favorite.getCount();
            count = count > FAVORITE_APP_COUNT ? FAVORITE_APP_COUNT : count;
            for (int i = 0; i < count; i++) {
                favorite.moveToPosition(i);
                if (favorite.getInt(1) == 0) {
                    break;
                }
                long id = favorite.getLong(0);
                if (!newFavorites.contains(id)) {
                    Cursor c = mContext.getContentResolver().query(Favorites.CONTENT_URI_NO_NOTIFICATION,
                            new String[]{Favorites._ID}, Favorites._ID + "=" + id , null, null);
                    if (c != null && c.getCount() > 0) {
                        newFavorites.add(id);
                    }

                    if (c != null) {
                        c.close();
                    }
                }
              }
            favorite.close();
        }
    }
}
