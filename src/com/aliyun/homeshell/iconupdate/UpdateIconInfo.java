package com.aliyun.homeshell.iconupdate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.aliyun.homeshell.LauncherSettings;

import android.content.ComponentName;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.util.Log;

public class UpdateIconInfo {
    int id = -1;
    ComponentName mComponentName = null;
    Bitmap mUpdateIcon = null;
    String mStartTime = null;
    String mEndTime = null;

    public UpdateIconInfo() {
    }

    public UpdateIconInfo(ComponentName component, String startTime, String endTime, Bitmap icon) {
        mComponentName = component;
        mStartTime = startTime;
        mEndTime = endTime;
        mUpdateIcon = icon;
    }

    public void setIcon(Bitmap icon) {
        mUpdateIcon = icon;
    }

    public Bitmap getIcon() {
        return mUpdateIcon;
    }

    public void setStartTime(String startTime) {
        mStartTime = startTime;
    }
    
    public String getStartTime() {
        return mStartTime;
    }

    public void setEndTime(String endTime) {
        mEndTime = endTime;
    }
    
    public void setComponentName(ComponentName cmp) {
        mComponentName = cmp;
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public void onAddToDatabase(ContentValues values) {
        values.put(IconUpdateManager.COMPONENTNAME,
                mComponentName.flattenToString());
        values.put(IconUpdateManager.STARTTIME, mStartTime);
        values.put(IconUpdateManager.ENDTIME, mEndTime);
        writeBitmap(values, mUpdateIcon);
    }

    private void writeBitmap(ContentValues values, Bitmap bitmap) {
        if (bitmap != null) {
            byte[] data = flattenBitmap(bitmap);
            values.put(LauncherSettings.Favorites.ICON, data);
        }
    }

//    private void writeBitmap(ContentValues values, Drawable bitmap) {
//        if (bitmap != null) {
//            Bitmap src = null;
//            if (bitmap instanceof FastBitmapDrawable) {
//                src = ((FastBitmapDrawable) bitmap).getBitmap();
//            } else {
//                src = ((BitmapDrawable) bitmap.getCurrent()).getBitmap();
//            }
//            writeBitmap(values, src);
//        }
//    }

    private byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        // BugID:5241319:out of memory exception
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream(size);
        } catch (OutOfMemoryError ex) {
            Log.e("ItemInfo", ex.toString());
            return null;
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("Favorite", "Could not write icon");
            return null;
        }
    }

    public String toString() {
        return "UpdateIcon info: componentName is " +
            mComponentName == null ? "null " : mComponentName.flattenToString() +
            " startTime is " +
            mStartTime == null ? "null " : mStartTime +
            " endTime is " +
            mEndTime == null ? "null " : mEndTime;
    }
}