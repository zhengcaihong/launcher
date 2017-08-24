package com.aliyun.homeshell.smartlocate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.database.AbstractCursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import app.aliyun.aml.FancyDrawable;

import com.aliyun.homeshell.AgedModeUtil;
import com.aliyun.homeshell.LauncherApplication;
import com.aliyun.homeshell.ShortcutInfo;
import com.aliyun.homeshell.appclone.AppCloneManager;
import com.aliyun.homeshell.icon.BubbleResources;
import com.aliyun.homeshell.icon.IconManager;

public class SuggestedAppsCursor extends AbstractCursor {

    private String[] columnNames = null;
    private int allDataCnt = 0;

    private List<SuggestedAppsItem> allDatas = null;
    private SuggestedAppsItem oneLineData = null;
    private IconManager mIconManager;

    private static final String TAG = "SuggestedAppsCursor";

    private boolean mSupportCardIcon = false;

    public SuggestedAppsCursor(String[] columnNames, List<ShortcutInfo> lstData, boolean onlyCount) {
        super();
        this.columnNames = columnNames;
        allDataCnt = lstData.size();
        if (!onlyCount) {
            mIconManager = ((LauncherApplication) LauncherApplication.getContext()).getIconManager();
            mSupportCardIcon = mIconManager.supprtCardIcon();
            transforData(lstData);
        }
    }

    private void transforData(List<ShortcutInfo> lstData) {
        if (allDatas == null) {
            allDatas = new ArrayList<SuggestedAppsItem>(allDataCnt);
        }
        allDatas.clear();

        for (ShortcutInfo info : lstData) {
            SuggestedAppsItem item = new SuggestedAppsItem();
            item.id = SuggestedAppsItem.generateId();
            Drawable drawable = null;
            if (mSupportCardIcon) {
                drawable = mIconManager.getAppCardBackgroud(info);
            } else {
                drawable = info.mIcon;
            }
            item.title = info.title.toString();
            if (info.mIcon instanceof FancyDrawable) {
                String label = ((FancyDrawable) drawable).getVariableString("app_label");
                if (label != null) {
                    item.title = label;
                }
            }
            item.intent = info.intent.toUri(0);
            item.isCardMode = mSupportCardIcon;
            /* YUNOS BEGIN */
            // ## date: 2016/06/27 ## author: yongxing.lyx
            // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
            item.userId = info.userId;
            if (mSupportCardIcon) {
                item.blob = iconDrawableToString(drawable, item.title, info.userId);
            } else {
                item.blob = iconDrawableToString(drawable, item.title, info.userId);
            }
            /* YUNOS END */
            Log.d(TAG, "transforData item " + item);
            allDatas.add(item);
        }
    }

    @Override
    public int getCount() {
        return allDataCnt;
    }

    @Override
    public int getType(int column) {
        String columnName = columnNames[column];
        if (SuggestedAppsProvider.COLUMN_ID.equals(columnName)) {
            return FIELD_TYPE_INTEGER;
        }
        if (SuggestedAppsProvider.COLUMN_ICON.equals(columnName)) {
            return FIELD_TYPE_BLOB;
        }
        if (SuggestedAppsProvider.COLUMN_TITLE.equals(columnName)) {
            return FIELD_TYPE_STRING;
        }
        if (SuggestedAppsProvider.COLUMN_INTENT.equals(columnName)) {
            return FIELD_TYPE_STRING;
        }
        if (SuggestedAppsProvider.COLUMN_CARDMODE.equals(columnName)) {
            return FIELD_TYPE_STRING;
        }
        /* YUNOS BEGIN */
        // ## date: 2016/06/27 ## author: yongxing.lyx
        // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
        if (SuggestedAppsProvider.COLUMN_USERID.equals(columnName)) {
            return FIELD_TYPE_STRING;
        }
        /* YUNOS END */
        return super.getType(column);
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        if (newPosition < 0 || newPosition >= getCount()) {
            return false;
        }

        oneLineData = allDatas.get(newPosition);
        Log.d(TAG, "onMove newPosition " + newPosition + " oneLineData " + oneLineData);
        return super.onMove(oldPosition, newPosition);
    }

    @Override
    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public byte[] getBlob(int column) {
        if (oneLineData == null) {
            return null;
        }
        if (SuggestedAppsProvider.COLUMN_ICON.equals(columnNames[column])) {
            return oneLineData.blob;
        }
        return super.getBlob(column);
    }

    @Override
    public String getString(int column) {
        if (oneLineData == null) {
            return null;
        }
        if (SuggestedAppsProvider.COLUMN_TITLE.equals(columnNames[column])) {
            return oneLineData.title;
        }
        if (SuggestedAppsProvider.COLUMN_INTENT.equals(columnNames[column])) {
            return oneLineData.intent;
        }

        if (SuggestedAppsProvider.COLUMN_CARDMODE.equals(columnNames[column])) {
            return String.valueOf(oneLineData.isCardMode);
        }
        // ## date: 2016/06/27 ## author: yongxing.lyx
        // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
        if (SuggestedAppsProvider.COLUMN_USERID.equals(columnNames[column])) {
            return Integer.toString(oneLineData.userId);
        }
        /* YUNOS END */
        return null;
    }

    @Override
    public int getColumnIndex(String columnName) {
        return super.getColumnIndex(columnName);
    }

    @Override
    public int getInt(int column) {
        if (oneLineData == null) {
            return 0;
        }
        if (SuggestedAppsProvider.COLUMN_ID.equals(columnNames[column])) {
            return oneLineData.id;
        }
        /* YUNOS BEGIN */
        // ## date: 2016/06/27 ## author: yongxing.lyx
        // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
        if (SuggestedAppsProvider.COLUMN_USERID.equals(columnNames[column])) {
            return oneLineData.userId;
        }
        /* YUNOS END */
        return 0;
    }

    @Override
    public short getShort(int column) {
        return 0;
    }

    @Override
    public long getLong(int column) {
        if (oneLineData == null) {
            return 0;
        }
        if (SuggestedAppsProvider.COLUMN_ID.equals(columnNames[column])) {
            return oneLineData.id;
        }
        return 0;
    }

    @Override
    public float getFloat(int column) {
        return 0;
    }

    @Override
    public double getDouble(int column) {
        return 0;
    }

    @Override
    public boolean isNull(int column) {
        return false;
    }

    private byte[] iconDrawableToString(Drawable drawable, String title, int userId) {
        if (drawable == null) {
            return null;
        }
        Resources res = LauncherApplication.getContext().getResources();
        int width = BubbleResources.getIconWidth(res);
        int height = BubbleResources.getIconHeight(res);
        if (drawable instanceof com.aliyun.homeshell.FastBitmapDrawable) {
            com.aliyun.homeshell.FastBitmapDrawable fbd = (com.aliyun.homeshell.FastBitmapDrawable) drawable;
            /* YUNOS BEGIN */
            // ## date: 2016/06/27 ## author: yongxing.lyx
            // ## BugID: 8402623: add clone mark to xiaoyun suggested icon.
            if (userId > 0) {
                int dstW;
                int dstH;
                int index = AppCloneManager.getMarkIconIndex(userId, null);
                Bitmap mark = BubbleResources.getAppCloneMarkIcon(LauncherApplication.getContext()
                        .getResources(), index);
                Bitmap srcBmp = fbd.getBitmap();
                Rect oldBounds = new Rect();
                Rect bounds = new Rect();

                if (mSupportCardIcon) {
                    dstW = drawable.getIntrinsicWidth();
                    dstH = drawable.getIntrinsicHeight();
                    bounds.set(0, 0, dstW, dstH);
                } else {
                    /* YUNOS BEGIN */
                    // ## date: 2016/09/26 ## author: yongxing.lyx
                    // ## BugID:8886139:cloned icons display out of bounds.
                    dstW = drawable.getIntrinsicWidth() + mark.getWidth() / 4;
                    dstH = drawable.getIntrinsicHeight() + mark.getWidth() / 4;
                    /* YUNOS END */
                    bounds.set(0, 0, drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight());
                }

                Bitmap dstBmp = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888);
                Canvas cvs = new Canvas();
                cvs.setBitmap(dstBmp);

                Drawable.Callback callback = drawable.getCallback();
                drawable.setCallback(null);
                oldBounds = drawable.getBounds();
                drawable.setBounds(bounds);
                drawable.draw(cvs);
                drawable.setBounds(oldBounds);
                drawable.setCallback(callback);

                cvs.drawBitmap(mark, dstW - mark.getWidth(), dstH - mark.getHeight(), null);
                cvs.setBitmap(null);
                byte[] bytes = iconBitmapToBytes(dstBmp, title);
                if (dstBmp != null && !dstBmp.isRecycled()) {
                    dstBmp.recycle();
                }
                return bytes;
            /* YUNOS END */
            } else if (AgedModeUtil.isAgedMode() && mSupportCardIcon) {

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                /* YUNOS BEGIN */
                // ## date: 2016/06/27 ## author: yongxing.lyx
                // ## BugID: 8402623: fix CalledFromWrongThreadException
                Drawable.Callback callback = drawable.getCallback();
                drawable.setCallback(null);
                Rect oldBounds = drawable.getBounds();
                fbd.setBounds(0, 0, width, height);
                drawable.draw(new Canvas(bitmap));
                drawable.setBounds(oldBounds);
                drawable.setCallback(callback);
                /* YUNOS END */
                byte[] bytes = iconBitmapToBytes(bitmap, title);
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return bytes;
            } else {
                return iconBitmapToBytes(fbd.getBitmap(), title);
            }
        } else if (drawable instanceof FancyDrawable) {
            FancyDrawable fd = (FancyDrawable) drawable;
            BitmapDrawable bd = (BitmapDrawable) fd.getCurrent();
            Bitmap fancyBitmap = bd.getBitmap();
            byte[] bytes = null;
            if (fancyBitmap.getWidth() != fd.getIntrinsicWidth() || fancyBitmap.getHeight() != fd.getIntrinsicHeight()) {
                if (AgedModeUtil.isAgedMode() && !mSupportCardIcon) {
                    // for L branch default theme
                    Bitmap bitmap = Bitmap.createBitmap(fancyBitmap, 0, 0, fd.getIntrinsicWidth(), fd.getIntrinsicHeight());
                    bytes = iconBitmapToBytes(bitmap, title);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                } else {
                    // if fancyBitmap and FancyDrawable's size are not equal
                    Bitmap bitmap = Bitmap.createBitmap(fd.getIntrinsicWidth(), fd.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    if (fancyBitmap.getWidth() < fd.getIntrinsicWidth() || fancyBitmap.getHeight() < fd.getIntrinsicHeight()) {
                        float sx = ((float) fd.getIntrinsicWidth()) / fancyBitmap.getWidth();
                        float sy = ((float) fd.getIntrinsicHeight()) / fancyBitmap.getHeight();
                        canvas.scale(sx, sy);
                    }
                    fd.draw(canvas);
                    bytes = iconBitmapToBytes(bitmap, title);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            } else {
                bytes = iconBitmapToBytes(fancyBitmap, title);
            }
            return bytes;
        } else if (drawable instanceof app.aliyun.graphics.FastBitmapDrawable) {
            app.aliyun.graphics.FastBitmapDrawable fbd = (app.aliyun.graphics.FastBitmapDrawable) drawable;
            return iconBitmapToBytes(fbd.getBitmap(), title);
        }
        return null;
    }

    private byte[] iconBitmapToBytes(Bitmap bitmap, String title) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bytes = baos.toByteArray();
        try {
            baos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
