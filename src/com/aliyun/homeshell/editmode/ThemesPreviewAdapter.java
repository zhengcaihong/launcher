
package com.aliyun.homeshell.editmode;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;

public class ThemesPreviewAdapter extends ThemeWallpaperBaseAdapter {
    private static final String TAG = "ThemesPreviewAdapter";

    private final float mHotseatHeightPercentage;

    public ThemesPreviewAdapter(Context context) {
        super(context, ThemeWallpaperBaseAdapter.DATACHANGE_TYPE_THEME);
        mHotseatHeightPercentage = 0.26f; // see translatePreviewImage() method
    }

    @Override
    protected void populateItem(BaseAttr item, Cursor c) {
        ThemeAttr theme = (ThemeAttr) item;
        theme.id = c.getString(c.getColumnIndex(ThemeWallpaperBaseAdapter.ID));
        //theme.isSystem = c.getInt(c.getColumnIndex(ThemeWallpaperBaseAdapter.IS_SYSTEM)) == 1;
        theme.name = c.getString(c.getColumnIndex(ThemeWallpaperBaseAdapter.NAME));
        theme.packageName = c.getString(c.getColumnIndex(ThemeWallpaperBaseAdapter.PACKAGE_NAME));
        byte[] data = c.getBlob(c.getColumnIndex(ThemeWallpaperBaseAdapter.THUMBNAIL));
        item.thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length);
        theme.checked = c.getInt(c.getColumnIndex(IS_CHECKED)) == 1;
    }

    @Override
    protected BaseAttr getNewItem() {
        return new ThemeAttr();
    }

    class ThemeAttr extends BaseAttr {
        public String packageName;

        @Override
        public String toString() {
            return "ThemeAttr [packageName=" + packageName + ", id=" + id + ", name=" + name + ", checked=" + checked + "]";
        }
    }

    @Override
    protected PointF translatePreviewImage(Bitmap thumbnail, float scale) {
        // ##date:2015/6/19 ##author:zhanggong.zg ##BugID:6095218
        // align theme preview image to workspace (clip hotseat)
        float y = mImageViewHeight - (thumbnail.getHeight()) * scale
                + mImageViewHeight * mHotseatHeightPercentage;
        return new PointF(0, y);
    }

}
